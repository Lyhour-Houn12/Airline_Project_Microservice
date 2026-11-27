package com.jing.booking.service.impl;

import com.jing.booking.client.RemoteLookup;
import com.jing.booking.mapper.BookingSegmentMapper;
import com.jing.booking.model.Booking;
import com.jing.booking.model.BookingSegment;
import com.jing.booking.model.PassengerSegment;
import com.jing.booking.payload.request.BookingSegmentFilter;
import com.jing.booking.payload.request.BookingSegmentRequest;
import com.jing.booking.payload.response.BookingSegmentResponse;
import com.jing.booking.repository.BookingRepository;
import com.jing.booking.repository.BookingSegmentRepository;
import com.jing.booking.service.BookingSegmentService;
import com.jing.booking.service.FareCalculator;
import com.jing.booking.service.SeatInventory;
import com.jing.booking.specification.BookingSegmentSpecs;
import com.jing.booking.utils.AccessGuard;
import com.jing.booking.validatation.BookingSegmentValidator;
import com.jing.common.dto.flight.FlightInstanceResponse;
import com.jing.common.dto.pricing.FareResponse;
import com.jing.common.enumeration.BookingStatus;
import com.jing.common.enumeration.FlightStatus;
import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.ResourceNotFoundException;
import com.jing.common.pagination.PageResponse;
import com.jing.common.pagination.PageableFactory;
import com.jing.common.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BookingSegmentServiceImpl implements BookingSegmentService {

    private static final Set<String> SORTABLE =
            Set.of("id", "segmentOrder", "departureTime", "arrivalTime", "flightNumber");
    private static final Set<FlightStatus> SELLABLE = EnumSet.of(FlightStatus.SCHEDULED, FlightStatus.DELAYED);

    private final BookingSegmentRepository bookingSegmentRepository;
    private final BookingRepository bookingRepository;
    private final BookingSegmentValidator bookingSegmentValidator;
    private final AccessGuard accessGuard;
    private final RemoteLookup remoteLookup;
    private final FareCalculator fareCalculator;
    private final SeatInventory seatInventory;

    @Override
    @Transactional
    public BookingSegmentResponse addSegment(Long bookingId, BookingSegmentRequest request, CurrentUser user) {
        Booking booking = loadForWrite(bookingId, user);
        requirePending(booking);

        // the flight and the price come from their owners, never from the client
        FlightInstanceResponse flight = remoteLookup.requireFlightInstance(request.flightInstanceId());
        FareResponse fare = remoteLookup.requireFare(request.fareId());
        requireSellable(flight);
        requireMatchingFare(booking, flight, fare);

        BookingSegment segment = BookingSegmentMapper.toEntity(flight, fare);
        FareCalculator.snapshot(segment, fare);
        int journey = bookingSegmentValidator.validate(booking, segment, request.journeyNumber());
        segment.setJourneyNumber(journey);
        segment.setSegmentOrder(booking.getSegments().size() + 1);

        booking.addSegment(segment);
        booking.getPassengers().forEach(p -> fareCalculator.price(booking.link(p, segment)));
        booking.recalculateTotal();
        bookingRepository.flush();
        return BookingSegmentMapper.toResponse(segment);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingSegmentResponse> getSegments(Long bookingId, BookingSegmentFilter filter,
                                                            int page, int size, String sortBy, String sortDirection,
                                                            CurrentUser user) {
        loadForRead(bookingId, user);   // existence + access check (404 otherwise)
        if (filter != null && filter.departureFrom() != null && filter.departureTo() != null
                && filter.departureFrom().isAfter(filter.departureTo()))
            throw new BadRequestException("departureFrom must not be after departureTo");
        Pageable pageable = PageableFactory.of(page, size, sortBy, sortDirection, SORTABLE, "segmentOrder");
        Page<BookingSegment> result = bookingSegmentRepository.findAll(BookingSegmentSpecs.of(bookingId, filter), pageable);
        return PageResponse.from(result.map(BookingSegmentMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public BookingSegmentResponse getSegment(Long bookingId, Long segmentId, CurrentUser user) {
        Booking booking = loadForRead(bookingId, user);
        return BookingSegmentMapper.toResponse(findSegment(booking, segmentId));
    }

    @Override
    @Transactional
    public void deleteSegment(Long bookingId, Long segmentId, CurrentUser user) {
        Booking booking = loadForWrite(bookingId, user);
        requirePending(booking);
        BookingSegment segment = findSegment(booking, segmentId);

        // check BEFORE changing anything, so a rejected delete leaves the booking untouched
        List<BookingSegment> remaining = booking.getSegments().stream()
                .filter(s -> !s.getId().equals(segmentId)).toList();
        bookingSegmentValidator.validateAfterRemoval(booking, remaining);

        List<Long> seats = segment.getPassengerSegments().stream()
                .map(PassengerSegment::getSeatInstanceId).filter(Objects::nonNull).toList();
        booking.removeSegment(segment);
        bookingRepository.flush();          // delete first, so renumbering can't hit the unique constraint
        renumber(booking);
        booking.recalculateTotal();
        seatInventory.releaseAfterCommit(booking, seats);
    }

    private void requireSellable(FlightInstanceResponse flight) {
        if (!Boolean.TRUE.equals(flight.getIsActive()) || !SELLABLE.contains(flight.getStatus())) {
            throw new BadRequestException("Flight " + flight.getFlightNumber() + " is not open for booking ("
                    + flight.getStatus() + ")");
        }
        LocalDate departureDay = flight.getDepartureDateTime().toLocalDate();
        LocalDate today = LocalDate.now();
        if (flight.getMinAdvanceBookingDays() != null
                && departureDay.isBefore(today.plusDays(flight.getMinAdvanceBookingDays()))) {
            throw new BadRequestException("Flight " + flight.getFlightNumber() + " must be booked at least "
                    + flight.getMinAdvanceBookingDays() + " day(s) before departure");
        }
        if (flight.getMaxAdvanceBookingDays() != null
                && departureDay.isAfter(today.plusDays(flight.getMaxAdvanceBookingDays()))) {
            throw new BadRequestException("Flight " + flight.getFlightNumber() + " can be booked at most "
                    + flight.getMaxAdvanceBookingDays() + " day(s) before departure");
        }
        if (!flight.getDepartureDateTime().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Flight " + flight.getFlightNumber() + " has already departed");
        }
    }

    private void requireMatchingFare(Booking booking, FlightInstanceResponse flight, FareResponse fare) {
        if (!Objects.equals(fare.getFlightId(), flight.getFlightId())) {
            throw new BadRequestException("Fare " + fare.getId() + " is not sold on flight " + flight.getFlightNumber());
        }
        if (!booking.getCurrency().equalsIgnoreCase(fare.getCurrency())) {
            throw new BadRequestException("Fare " + fare.getId() + " is priced in " + fare.getCurrency()
                    + " but this booking is in " + booking.getCurrency());
        }
    }

    private void renumber(Booking booking) {
        int order = 1, journey = 0, previousOriginal = -1;
        for (BookingSegment s : booking.getSegments()) {
            int original = s.getJourneyNumber();
            if (original != previousOriginal) { journey++; previousOriginal = original; }
            s.setJourneyNumber(journey);    // closes gaps when a whole journey disappears
            s.setSegmentOrder(order++);
        }
    }

    private void requirePending(Booking booking) {
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Flights can only be changed while the booking is PENDING (current: "
                    + booking.getStatus() + ")");
        }
        if (booking.isExpired(LocalDateTime.now())) {
            throw new BadRequestException("The booking has expired. Please start a new one");
        }
    }

    private BookingSegment findSegment(Booking booking, Long segmentId) {
        return booking.getSegments().stream()
                .filter(s -> s.getId().equals(segmentId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Booking Segment", "id", segmentId));
    }

    private Booking loadForRead(Long bookingId, CurrentUser user) {
        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
        accessGuard.requireCanView(b, user);
        return b;
    }

    private Booking loadForWrite(Long bookingId, CurrentUser user) {
        Booking b = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
        accessGuard.requireBookingOwner(b, user);
        return b;
    }
}
