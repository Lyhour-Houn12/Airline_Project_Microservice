package com.jing.seat.service.impl;

import com.jing.common.dto.seat.SeatConfirmRequest;
import com.jing.common.dto.seat.SeatConfirmResponse;
import com.jing.common.dto.seat.SeatHoldRequest;
import com.jing.common.dto.seat.SeatInstanceResponse;
import com.jing.common.dto.seat.SeatReleaseRequest;
import com.jing.common.enumeration.SeatAvailabilityStatus;
import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.DuplicateResourceException;
import com.jing.common.exception.ResourceNotFoundException;
import com.jing.seat.model.FlightInstanceCabin;
import com.jing.seat.model.SeatInstance;
import com.jing.seat.repository.SeatInstanceRepository;
import com.jing.seat.service.SeatHoldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SeatHoldServiceImpl implements SeatHoldService {

    private final SeatInstanceRepository seatInstanceRepository;

    @Override
    public SeatInstanceResponse hold(Long seatInstanceId, SeatHoldRequest request) {
        SeatInstance si = seatInstanceRepository.findDetailById(seatInstanceId)
                .orElseThrow(() -> new ResourceNotFoundException("SeatInstance", "id", seatInstanceId));

        if (!si.getFlightInstanceId().equals(request.flightInstanceId())) {
            throw new BadRequestException("Seat " + si.getSeat().getSeatNumber() + " is not on this flight");
        }
        if (!si.getFlightInstanceCabin().getCabinClass().getId().equals(request.cabinClassId())) {
            throw new BadRequestException("Seat " + si.getSeat().getSeatNumber()
                    + " is in " + si.getFlightInstanceCabin().getCabinClass().getName()
                    + ", not in the cabin of this fare");
        }
        if (!Boolean.TRUE.equals(si.getFlightInstanceCabin().getIsBookable())) {
            throw new BadRequestException("This cabin is not bookable");
        }

        try {
            si.hold(request.holdReference(), request.holdUntil(), LocalDateTime.now());
        } catch (IllegalStateException e) {
            // 409: someone else has it; the client should pick another seat
            throw new DuplicateResourceException(e.getMessage());
        }
        // @Version on SeatInstance: if two bookings race for the same seat, the second one gets a 409
        seatInstanceRepository.saveAndFlush(si);
        return SeatInstanceServiceImpl.toResponse(si);
    }

    @Override
    public SeatConfirmResponse confirm(SeatConfirmRequest request) {
        Set<Long> wanted = new HashSet<>(request.seatInstanceIds());
        List<Long> confirmed = new ArrayList<>();
        List<Long> rejected = new ArrayList<>();

        for (SeatInstance si : seatInstanceRepository.findAllById(wanted)) {
            boolean wasHeld = si.isHeldBy(request.holdReference());
            if (si.confirm(request.holdReference())) {
                confirmed.add(si.getId());
                if (wasHeld) {
                    FlightInstanceCabin cabin = si.getFlightInstanceCabin();
                    cabin.setBookedSeats(cabin.getBookedSeats() + 1);
                }
            } else {
                rejected.add(si.getId());
            }
            wanted.remove(si.getId());
        }
        rejected.addAll(wanted);   // ids that don't exist

        if (!rejected.isEmpty()) {
            log.warn("Booking {} lost its hold on seat instances {}", request.holdReference(), rejected);
        }
        return new SeatConfirmResponse(confirmed, rejected);
    }

    @Override
    public int release(SeatReleaseRequest request) {
        List<SeatInstance> seats = seatInstanceRepository.findAllByHoldReference(request.holdReference());
        Set<Long> only = request.seatInstanceIds() == null || request.seatInstanceIds().isEmpty()
                ? null : new HashSet<>(request.seatInstanceIds());

        int released = 0;
        for (SeatInstance si : seats) {
            if (only != null && !only.contains(si.getId())) continue;
            SeatAvailabilityStatus before = si.release(request.holdReference());
            if (before == null) continue;
            released++;
            if (before == SeatAvailabilityStatus.BOOKED) {
                FlightInstanceCabin cabin = si.getFlightInstanceCabin();
                cabin.setBookedSeats(Math.max(0, cabin.getBookedSeats() - 1));
            }
        }
        log.info("Released {} seat(s) of booking {}", released, request.holdReference());
        return released;
    }
}
