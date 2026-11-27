package com.jing.booking.service.impl;

import com.jing.booking.mapper.PassengerSegmentMapper;
import com.jing.booking.model.Booking;
import com.jing.booking.model.PassengerSegment;
import com.jing.common.security.CurrentUser;
import com.jing.booking.payload.request.PassengerSegmentFilter;
import com.jing.booking.payload.request.SeatAssignRequest;
import com.jing.booking.payload.response.PassengerSegmentResponse;
import com.jing.booking.repository.BookingRepository;
import com.jing.booking.repository.PassengerSegmentRepository;
import com.jing.booking.service.PassengerSegmentService;
import com.jing.booking.specification.PassengerSegmentSpecs;
import com.jing.booking.utils.AccessGuard;
import com.jing.common.pagination.PageableFactory;
import com.jing.common.enumeration.BookingStatus;
import com.jing.common.enumeration.PassengerSegmentStatus;
import com.jing.common.enumeration.PassengerType;
import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.ResourceNotFoundException;
import com.jing.common.pagination.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PassengerSegmentServiceImpl implements PassengerSegmentService {
    private final BookingRepository bookingRepository;
    private final PassengerSegmentRepository passengerSegmentRepository;
    private final AccessGuard accessGuard;

    private static final Set<String> SORTABLE = Set.of(
            "id", "status", "seatInstanceId", "baseFare", "segment.segmentOrder", "passenger.lastName");

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
        return PassengerSegmentMapper.toResponse(find(booking, id));    }

    @Override
    @Transactional
    public PassengerSegmentResponse assignSeat(Long bookingId, Long id, SeatAssignRequest request, CurrentUser user) {
        Booking booking = loadForWrite(bookingId, user);
        requirePending(booking);

        PassengerSegment ps = find(booking, id);

        if (ps.getStatus() == PassengerSegmentStatus.CANCELLED){
            throw new BadRequestException("Cannot assign a seat to a cancelled passenger segment");
        }

        if (ps.getPassenger().getPassengerType() == PassengerType.INFANT)
            throw new BadRequestException("Infants do not get their own seat");

        boolean taken = allOf(booking)
                .filter(other -> !other.getId().equals(ps.getId()))
                .filter(other -> other.getSegment().getId().equals(ps.getSegment().getId()))
                .anyMatch(other -> Objects.equals(other.getSeatInstanceId(), request.seatInstanceId()));
        if (taken)
            throw new BadRequestException("That seat is already assigned to another passenger on this segment");

        // TODO (Feign): validate the seat instance and really hold it in seat-service
        ps.setSeatInstanceId(request.seatInstanceId());
        ps.setStatus(PassengerSegmentStatus.SEAT_HELD);
        return PassengerSegmentMapper.toResponse(ps);
    }

    @Override
    @Transactional
    public PassengerSegmentResponse clearSeat(Long bookingId, Long id, CurrentUser user) {
        Booking booking = loadForWrite(bookingId, user);
        requirePending(booking);
        PassengerSegment ps = find(booking, id);

        // TODO (Feign): release the hold in seat-service
        ps.setSeatInstanceId(null);
        if (ps.getStatus() != PassengerSegmentStatus.CANCELLED)
            ps.setStatus(PassengerSegmentStatus.NOT_SEATED);
        return PassengerSegmentMapper.toResponse(ps);
    }

    private Stream<PassengerSegment> allOf(Booking booking) {
        return booking.getSegments().stream().flatMap(s -> s.getPassengerSegments().stream());
    }


    private PassengerSegment find(Booking booking, Long id) {
        return allOf(booking).filter(ps -> ps.getId().equals(id)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Passenger segment", "id", id));
    }

    private void requirePending(Booking booking) {
        if (booking.getStatus() != BookingStatus.PENDING)
            throw new BadRequestException(
                    "Seats can only be changed while the booking is PENDING (current: " + booking.getStatus() + ")");
    }

    private Booking loadForRead(Long bookingId, CurrentUser user) {
        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
        accessGuard.requireCanView(b, user);
        return b;
    }

    private Booking loadForWrite(Long bookingId, CurrentUser user) {
        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
        accessGuard.requireBookingOwner(b, user);
        return b;
    }
}
