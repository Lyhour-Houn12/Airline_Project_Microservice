package com.jing.booking.service.impl;

import com.jing.booking.mapper.PassengerSegmentMapper;
import com.jing.booking.model.Booking;
import com.jing.booking.model.PassengerSegment;
import com.jing.booking.payload.request.PassengerSegmentFilter;
import com.jing.booking.payload.request.SeatAssignRequest;
import com.jing.booking.payload.response.PassengerSegmentResponse;
import com.jing.booking.repository.BookingRepository;
import com.jing.booking.repository.PassengerSegmentRepository;
import com.jing.booking.service.PassengerSegmentService;
import com.jing.booking.service.SeatInventory;
import com.jing.booking.specification.PassengerSegmentSpecs;
import com.jing.booking.utils.AccessGuard;
import com.jing.common.dto.seat.SeatInstanceResponse;
import com.jing.common.enumeration.BookingStatus;
import com.jing.common.enumeration.PassengerSegmentStatus;
import com.jing.common.enumeration.PassengerType;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PassengerSegmentServiceImpl implements PassengerSegmentService {

    private static final Set<String> SORTABLE = Set.of(
            "id", "status", "seatInstanceId", "baseFare", "segment.segmentOrder", "passenger.lastName");

    private final BookingRepository bookingRepository;
    private final PassengerSegmentRepository passengerSegmentRepository;
    private final AccessGuard accessGuard;
    private final SeatInventory seatInventory;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PassengerSegmentResponse> list(Long bookingId, PassengerSegmentFilter filter,
                                                       int page, int size, String sortBy, String sortDirection,
                                                       CurrentUser user) {
        loadForRead(bookingId, user);
        Pageable pageable = PageableFactory.of(page, size, sortBy, sortDirection, SORTABLE, "segment.segmentOrder");
        Page<PassengerSegment> result =
                passengerSegmentRepository.findAll(PassengerSegmentSpecs.of(bookingId, filter), pageable);
        return PageResponse.from(result.map(PassengerSegmentMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PassengerSegmentResponse get(Long bookingId, Long id, CurrentUser user) {
        Booking booking = loadForRead(bookingId, user);
        return PassengerSegmentMapper.toResponse(find(booking, id));
    }

    /**
     * Holds the seat in seat-service for this booking (until the booking expires). Picking another seat
     * releases the previous one. A seat someone else already holds gives a 409.
     */
    @Override
    @Transactional
    public PassengerSegmentResponse assignSeat(Long bookingId, Long id, SeatAssignRequest request, CurrentUser user) {
        Booking booking = loadForWrite(bookingId, user);
        requirePending(booking);
        PassengerSegment ps = find(booking, id);

        if (ps.getStatus() == PassengerSegmentStatus.CANCELLED) {
            throw new BadRequestException("Cannot assign a seat to a cancelled passenger segment");
        }
        if (ps.getPassenger().getPassengerType() == PassengerType.INFANT) {
            throw new BadRequestException("Infants do not get their own seat");
        }
        Long seatId = request.seatInstanceId();
        if (seatId.equals(ps.getSeatInstanceId())) {
            return PassengerSegmentMapper.toResponse(ps);          // already this seat
        }
        boolean takenOnBooking = ps.getSegment().getPassengerSegments().stream()
                .anyMatch(other -> !other.getId().equals(ps.getId()) && seatId.equals(other.getSeatInstanceId()));
        if (takenOnBooking) {
            throw new BadRequestException("That seat is already assigned to another passenger on this flight");
        }

        SeatInstanceResponse held = seatInventory.hold(booking, ps.getSegment(), seatId);   // 409 if taken

        Long previous = ps.getSeatInstanceId();
        ps.setSeatInstanceId(seatId);
        ps.setStatus(PassengerSegmentStatus.SEAT_HELD);
        ps.setSeatSurcharge(held.surcharge() != null ? held.surcharge() : BigDecimal.ZERO);
        booking.recalculateTotal();
        if (previous != null) {
            seatInventory.releaseAfterCommit(booking, List.of(previous));
        }
        return PassengerSegmentMapper.toResponse(ps);
    }

    @Override
    @Transactional
    public PassengerSegmentResponse clearSeat(Long bookingId, Long id, CurrentUser user) {
        Booking booking = loadForWrite(bookingId, user);
        requirePending(booking);
        PassengerSegment ps = find(booking, id);

        Long previous = ps.getSeatInstanceId();
        ps.setSeatInstanceId(null);
        ps.setSeatSurcharge(BigDecimal.ZERO);
        if (ps.getStatus() != PassengerSegmentStatus.CANCELLED) {
            ps.setStatus(PassengerSegmentStatus.NOT_SEATED);
        }
        booking.recalculateTotal();
        if (previous != null) {
            seatInventory.releaseAfterCommit(booking, List.of(previous));
        }
        return PassengerSegmentMapper.toResponse(ps);
    }

    private PassengerSegment find(Booking booking, Long id) {
        return booking.passengerSegments().filter(ps -> Objects.equals(ps.getId(), id)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Passenger segment", "id", id));
    }

    private void requirePending(Booking booking) {
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException(
                    "Seats can only be changed while the booking is PENDING (current: " + booking.getStatus() + ")");
        }
        if (booking.isExpired(LocalDateTime.now())) {
            throw new BadRequestException("The booking has expired. Please start a new one");
        }
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
