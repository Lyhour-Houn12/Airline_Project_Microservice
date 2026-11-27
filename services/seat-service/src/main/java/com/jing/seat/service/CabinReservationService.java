package com.jing.seat.service;

import com.jing.common.dto.seat.CabinConfirmRequest;
import com.jing.common.dto.seat.CabinReservationRequest;
import com.jing.common.dto.seat.CabinReservationResponse;
import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.DuplicateResourceException;
import com.jing.seat.model.CabinReservation;
import com.jing.seat.model.FlightInstanceCabin;
import com.jing.seat.repository.CabinReservationRepository;
import com.jing.seat.repository.FlightInstanceCabinRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Cabin capacity: how many places each booking takes in each cabin. Every decision locks the cabin's
 * row first, so two bookings can never both take the last place.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CabinReservationService {

    private final FlightInstanceCabinRepository cabins;
    private final CabinReservationRepository reservations;

    /** Sets the booking's places in a cabin. 409 when there aren't enough free places. */
    public CabinReservationResponse reserve(CabinReservationRequest request) {
        FlightInstanceCabin cabin = cabins.lockByFlightInstanceAndCabinClass(request.flightInstanceId(), request.cabinClassId())
                .orElseThrow(() -> new BadRequestException("This flight has no such cabin set up for sale"));
        if (!Boolean.TRUE.equals(cabin.getIsBookable())) {
            throw new BadRequestException(cabin.getCabinClass().getName() + " is not open for booking on this flight");
        }
        releaseLapsed(cabin, LocalDateTime.now());

        CabinReservation mine = reservations
                .findByFlightInstanceCabinAndHoldReference(cabin, request.holdReference()).orElse(null);
        if (mine != null && mine.getStatus() == CabinReservation.Status.BOOKED) {
            throw new BadRequestException("Booking " + request.holdReference() + " is already paid; its places can't change here");
        }
        int current = mine == null ? 0 : mine.getSeats();
        int delta = request.seats() - current;
        if (delta > cabin.getAvailableSeats()) {
            int free = Math.max(0, cabin.getAvailableSeats());
            throw new DuplicateResourceException(current == 0
                    ? "Only " + free + " seat(s) left in " + cabin.getCabinClass().getName() + " on this flight"
                    : "Not enough seats in " + cabin.getCabinClass().getName() + ": this booking needs " + delta
                      + " more, " + free + " left on this flight");
        }

        if (request.seats() == 0) {
            if (mine != null) reservations.delete(mine);
        } else {
            if (mine == null) {
                mine = CabinReservation.builder().flightInstanceCabin(cabin).holdReference(request.holdReference()).build();
            }
            mine.setSeats(request.seats());
            mine.setHeldUntil(request.holdUntil());
            reservations.save(mine);
        }
        cabin.changeHeld(delta);
        return new CabinReservationResponse(cabin.getId(), request.seats(), cabin.getAvailableSeats());
    }

    /** The booking is paid: its places become booked, also ones whose hold lapsed meanwhile. */
    public void confirm(CabinConfirmRequest request) {
        LocalDateTime now = LocalDateTime.now();
        for (CabinConfirmRequest.Item item : request.cabins()) {
            if (item.seats() <= 0) continue;
            FlightInstanceCabin cabin = cabins.lockByFlightInstanceAndCabinClass(item.flightInstanceId(), item.cabinClassId())
                    .orElse(null);
            if (cabin == null) {
                log.error("Booking {} is paid but cabin {} of flight instance {} no longer exists",
                        request.holdReference(), item.cabinClassId(), item.flightInstanceId());
                continue;
            }
            releaseLapsed(cabin, now, request.holdReference());   // other bookings' lapsed holds, not ours
            CabinReservation mine = reservations
                    .findByFlightInstanceCabinAndHoldReference(cabin, request.holdReference()).orElse(null);
            if (mine != null && mine.getStatus() == CabinReservation.Status.BOOKED) continue;   // repeat

            int held = mine == null ? 0 : mine.getSeats();
            int extra = item.seats() - held;                       // places we have to find now
            if (extra > cabin.getAvailableSeats()) {
                log.error("OVERBOOKED: paid booking {} needs {} more place(s) in {} of flight instance {}, only {} free",
                        request.holdReference(), extra, cabin.getCabinClass().getName(), item.flightInstanceId(),
                        cabin.getAvailableSeats());
            }
            if (mine == null) {
                mine = CabinReservation.builder().flightInstanceCabin(cabin).holdReference(request.holdReference()).build();
            }
            cabin.changeHeld(-held);
            cabin.changeBooked(item.seats());
            mine.setSeats(item.seats());
            mine.setStatus(CabinReservation.Status.BOOKED);
            mine.setHeldUntil(null);
            reservations.save(mine);
        }
    }

    /** Cancelled or expired: every place of this booking is given back. Returns how many. */
    public int releaseAll(String holdReference) {
        int released = 0;
        for (CabinReservation r : reservations.findByHoldReference(holdReference)) {
            FlightInstanceCabin cabin = cabins.lockById(r.getFlightInstanceCabin().getId()).orElseThrow();
            if (r.getStatus() == CabinReservation.Status.BOOKED) {
                cabin.changeBooked(-r.getSeats());
            } else {
                cabin.changeHeld(-r.getSeats());
            }
            released += r.getSeats();
            reservations.delete(r);
        }
        return released;
    }

    /** Holds of bookings that were never paid stop counting once they lapse. */
    private void releaseLapsed(FlightInstanceCabin cabin, LocalDateTime now) {
        releaseLapsed(cabin, now, null);
    }

    private void releaseLapsed(FlightInstanceCabin cabin, LocalDateTime now, String keep) {
        for (CabinReservation r : reservations.findByFlightInstanceCabinAndStatus(cabin, CabinReservation.Status.HELD)) {
            if (r.isLapsed(now) && !r.getHoldReference().equals(keep)) {
                cabin.changeHeld(-r.getSeats());
                reservations.delete(r);
                log.info("Released {} lapsed place(s) of booking {} in cabin {}", r.getSeats(), r.getHoldReference(), cabin.getId());
            }
        }
    }
}
