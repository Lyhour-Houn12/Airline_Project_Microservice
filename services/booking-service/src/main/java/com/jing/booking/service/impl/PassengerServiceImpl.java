package com.jing.booking.service.impl;

import com.jing.booking.model.PassengerSegment;
import com.jing.booking.service.FareCalculator;
import com.jing.booking.service.SeatInventory;

import com.jing.booking.mapper.PassengerMapper;
import com.jing.booking.model.Booking;
import com.jing.booking.model.Passenger;
import com.jing.common.security.CurrentUser;
import com.jing.booking.payload.request.PassengerRequest;
import com.jing.booking.payload.response.PassengerResponse;
import com.jing.booking.repository.BookingRepository;
import com.jing.booking.service.PassengerService;
import com.jing.booking.utils.AccessGuard;
import com.jing.booking.validatation.PassengerValidator;
import com.jing.common.enumeration.BookingStatus;
import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.ResourceNotFoundException;
import com.jing.common.pagination.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PassengerServiceImpl implements PassengerService {
    private static final int MAX_PASSENGER = 9;

    private final BookingRepository bookingRepository;
    private final PassengerValidator passengerValidator;
    private final AccessGuard accessGuard;
    private final FareCalculator fareCalculator;
    private final SeatInventory seatInventory;

    @Override
    public PassengerResponse add(Long bookingId, PassengerRequest request, CurrentUser user) {

        Booking booking = loadEditable(bookingId, user);

        if(booking.getPassengers().size() >= MAX_PASSENGER){
            throw new BadRequestException("A booking can have at most " + MAX_PASSENGER + " passengers");
        }

        Passenger accompanyingAdult = passengerValidator.validate(request, booking, null);
        Passenger passenger = PassengerMapper.toEntity(request, accompanyingAdult);

        booking.addPassenger(passenger);
        booking.getSegments().forEach(s -> fareCalculator.price(booking.link(passenger, s)));
        booking.recalculateTotal();

        bookingRepository.saveAndFlush(booking);           // flush so the new id exists
        return PassengerMapper.toResponse(passenger);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PassengerResponse> getAll(Long bookingId, CurrentUser user) {
        Booking booking = loadViewable(bookingId, user);

        return booking.getPassengers().stream()
                .sorted(Comparator.comparing(Passenger::getId))
                .map(PassengerMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PassengerResponse getById(Long bookingId, Long passengerId, CurrentUser user) {
        Booking booking = loadViewable(bookingId, user);
            return PassengerMapper.toResponse(findInBooking(booking, passengerId));
    }

    @Override
    public PassengerResponse update(Long bookingId, Long passengerId, PassengerRequest request, CurrentUser user) {
        Booking booking = loadEditable(bookingId, user);
        Passenger passenger = findInBooking(booking, passengerId);

        Passenger adultAccompanying =  passengerValidator.validate(request, booking, passenger);
        PassengerMapper.apply(passenger, request, adultAccompanying);
        // the passenger type may have changed (adult <-> child): price every flight again
        passenger.getPassengerSegments().forEach(fareCalculator::price);
        booking.recalculateTotal();

        bookingRepository.saveAndFlush(booking);
        return PassengerMapper.toResponse(passenger);
    }

    @Override
    public void delete(Long bookingId, Long passengerId, CurrentUser user) {
        Booking booking = loadEditable(bookingId, user);
        Passenger passenger = findInBooking(booking, passengerId);

        if (passengerValidator.hostsInfant(booking, passenger)) {
            throw new BadRequestException("This adult travels with an infant. Remove the infant first");
        }

        List<Long> seats = passenger.getPassengerSegments().stream()
                .map(PassengerSegment::getSeatInstanceId).filter(Objects::nonNull).toList();
        booking.removePassenger(passenger);
        booking.recalculateTotal();
        bookingRepository.saveAndFlush(booking);
        seatInventory.releaseAfterCommit(booking, seats);
    }


    /** Owner only, PENDING only, not expired. Locks the booking version against concurrent edits. */
    private Booking loadEditable(Long bookingId, CurrentUser user){
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        if (!booking.getUserId().equals(user.id())){
            throw new ResourceNotFoundException("Booking", "id", bookingId);
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Passenger can only be changed while the booking is PENDING");
        }

        if(booking.getExpiresAt() != null && booking.getExpiresAt().isBefore(LocalDateTime.now())){
            throw new BadRequestException("The booking has expired. Please start a new one");
        }

        return booking;
    }

    private Booking loadViewable(Long bookingId, CurrentUser user){
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
        if (!accessGuard.canViewBooking(booking, user)){
            throw new ResourceNotFoundException("Booking", "id", bookingId);
        }
        return booking;
    }

    private Passenger findInBooking(Booking booking, Long passengerId){
        return booking.getPassengers().stream()
                .filter(p -> p.getId().equals(passengerId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Passenger", "id", passengerId));
    }

}
