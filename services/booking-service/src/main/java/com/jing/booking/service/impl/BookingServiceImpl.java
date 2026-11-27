package com.jing.booking.service.impl;

import com.jing.booking.mapper.BookingMapper;
import com.jing.booking.model.Booking;
import com.jing.common.security.CurrentUser;
import com.jing.booking.payload.request.BookingFilter;
import com.jing.booking.payload.request.CancelBookingRequest;
import com.jing.booking.payload.request.ChangeStatusRequest;
import com.jing.booking.payload.request.CreateDraftBookingRequest;
import com.jing.booking.payload.response.BookingResponse;
import com.jing.booking.payload.response.BookingStatusHistoryResponse;
import com.jing.booking.repository.BookingRepository;
import com.jing.booking.service.BookingReferenceGenerator;
import com.jing.booking.service.BookingService;
import com.jing.booking.service.BookingStateMachine;
import com.jing.booking.specification.BookingSpecification;
import com.jing.booking.utils.AccessGuard;
import com.jing.common.pagination.PageableFactory;
import com.jing.common.enumeration.BookingStatus;
import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.ForbiddenException;
import com.jing.common.exception.DuplicateResourceException;
import com.jing.common.exception.ResourceNotFoundException;
import com.jing.common.pagination.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.jing.booking.service.BookingLifecycle;
import com.jing.booking.service.BookingProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BookingServiceImpl implements BookingService {
    private static final Set<String> SORTABLE = Set.of("bookingDate", "lastModified", "totalAmount", "status");

    private final BookingRepository bookingRepository;
    private final BookingReferenceGenerator bookingReferenceGenerator;
    private final BookingStateMachine bookingStateMachine;
    private final AccessGuard accessGuard;
    private final BookingLifecycle bookingLifecycle;
    private final BookingProperties properties;

    @Override
    @Transactional
    public BookingResponse createDraft(CreateDraftBookingRequest request, CurrentUser user, String idempotencyKey) {
        String key = (idempotencyKey == null || idempotencyKey.isBlank()) ? null : idempotencyKey.trim();

        if (key != null){
            Optional<Booking> existing = bookingRepository.findByIdempotencyKey(key);
            if(existing.isPresent()){
                Booking booking = existing.get();
                if(!booking.getUserId().equals(user.id())){
                    throw new DuplicateResourceException("Booking", "idempotency", key);
                }
                return BookingMapper.toResponse(booking);   // same request retried: return the original
            }

        }

        Booking booking = Booking.builder()
                .bookingReference(bookingReferenceGenerator.generator())
                .userId(user.id())
                .tripType(request.tripType())
                .status(BookingStatus.PENDING)
                .currency(normalizeCurrency(request.currency()))
                .flexibleTicket(request.flexibleTicket())
                .contactInfo(request.contactInfo())
                .expiresAt(LocalDateTime.now().plusMinutes(properties.holdMinutes()))
                .idempotencyKey(key)
                .build();

        booking.recordCreation(String.valueOf(user.id()), "Booking created");

        return BookingMapper.toResponse(bookingRepository.save(booking));
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getById(Long id, CurrentUser user) {
        return BookingMapper.toResponse(loadAccessible(id, user));
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getByReference(String bookingReference, CurrentUser user) {
        Booking booking = bookingRepository.findByBookingReference(bookingReference.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "reference", bookingReference));
        assertCanView(booking, user);
        return BookingMapper.toResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> search(BookingFilter filter,CurrentUser user, int page, int size, String sortBy, String sortDirection) {

        if (filter != null && filter.createdFrom() != null && filter.createdTo() != null
                && filter.createdFrom().isAfter(filter.createdTo())) {
            throw new BadRequestException("createdFrom must not be after createdTo");
        }

        return PageResponse.from(
                bookingRepository.findAll(BookingSpecification.withFilter(filter, user, accessGuard.visibleAirline(user).orElse(null)), PageableFactory.of(page, size, sortBy, sortDirection, SORTABLE, "bookingDate"))
                        .map(BookingMapper::toResponse)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingStatusHistoryResponse> getHistory(Long id, CurrentUser user) {
        Booking booking = loadAccessible(id, user);
        return booking.getHistory().stream().map(BookingMapper::toHistoryResponse).toList();
    }

    @Override
    @Transactional
    public BookingResponse cancel(Long id, CancelBookingRequest request, CurrentUser user) {
        Booking booking = loadAccessible(id, user);
        // seeing a booking is not enough to cancel it: airline owners may view bookings on their flights,
        // but only the customer who booked it (or an admin) may cancel
        if (!user.isAdmin() && !accessGuard.isBookingOwner(booking, user)) {
            throw new ForbiddenException("Only the customer who made this booking can cancel it");
        }

        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BadRequestException("Only PENDING or CONFIRMED bookings can be cancelled");
        }

        String reason = (request != null && request.reason() != null) ? request.reason() : "Cancelled by user";
        bookingLifecycle.close(booking, BookingStatus.CANCELLED, String.valueOf(user.id()), reason);
        return BookingMapper.toResponse(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public BookingResponse changeStatus(Long id, ChangeStatusRequest request, CurrentUser user) {
        accessGuard.requireAdmin(user, "change a booking status directly");
        Booking booking = bookingRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("booking", "id", id));

        if (booking.getStatus() == request.status()) {
            throw new BadRequestException("Booking is already " + request.status());
        }

        String by = "ADMIN:" + user.id();
        // statuses with side effects (seats, tickets) go through the lifecycle, the rest is a plain change
        switch (request.status()) {
            case CONFIRMED -> bookingLifecycle.confirm(booking, null, by);
            case CANCELLED, EXPIRED -> bookingLifecycle.close(booking, request.status(), by, request.reason());
            default -> {
                bookingStateMachine.assertTransition(booking.getStatus(), request.status());
                booking.changeStatus(request.status(), by, request.reason());
            }
        }
        return BookingMapper.toResponse(bookingRepository.save(booking));
    }


    private Booking loadAccessible(Long id, CurrentUser user) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("booking", "id", id));
        assertCanView(booking, user);
        return booking;
    }

    /** Someone else's booking looks exactly like a missing one, so ids can't be probed. */
    private void assertCanView(Booking booking, CurrentUser user) {
        if (!accessGuard.canViewBooking(booking, user)) {
            throw new ResourceNotFoundException("Booking", "id", booking.getId());
        }
    }


    private String normalizeCurrency(String raw){
        String code = raw.trim().toUpperCase();
        try{
            return Currency.getInstance(code).getCurrencyCode();
        }catch (IllegalArgumentException e){
            throw new BadRequestException("Invalid currency code: " + raw);
        }
    }
}
