package com.jing.seat.controller;

import com.jing.common.dto.seat.SeatConfirmRequest;
import com.jing.common.dto.seat.SeatConfirmResponse;
import com.jing.common.dto.seat.SeatHoldRequest;
import com.jing.common.dto.seat.SeatInstanceResponse;
import com.jing.common.dto.seat.SeatReleaseRequest;
import com.jing.seat.service.CabinReservationService;
import com.jing.seat.service.SeatHoldService;
import com.jing.common.dto.seat.CabinConfirmRequest;
import com.jing.common.dto.seat.CabinReservationRequest;
import com.jing.common.dto.seat.CabinReservationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Seat inventory used by booking-service only.
 *
 * <p>Everything under {@code /internal/**} is service-to-service: the API gateway must never route it,
 * so customers cannot hold or release seats directly, only through their booking.
 */
@RestController
@RequestMapping("/internal/v1/seat-instances")
@RequiredArgsConstructor
public class InternalSeatInventoryController {

    private final SeatHoldService seatHoldService;
    private final CabinReservationService cabinReservationService;

    @PostMapping("/{id}/hold")
    public SeatInstanceResponse hold(@PathVariable Long id, @Valid @RequestBody SeatHoldRequest request) {
        return seatHoldService.hold(id, request);
    }

    @PostMapping("/confirm")
    public SeatConfirmResponse confirm(@Valid @RequestBody SeatConfirmRequest request) {
        return seatHoldService.confirm(request);
    }

    @PostMapping("/release")
    public Map<String, Integer> release(@Valid @RequestBody SeatReleaseRequest request) {
        return Map.of("released", seatHoldService.release(request));
    }

    // ------------------------------------------------------------ cabin capacity (places, seat or not)

    @PostMapping("/cabin-reservations")
    public CabinReservationResponse reserve(@Valid @RequestBody CabinReservationRequest request) {
        return cabinReservationService.reserve(request);
    }

    @PostMapping("/cabin-reservations/confirm")
    public void confirmCabins(@Valid @RequestBody CabinConfirmRequest request) {
        cabinReservationService.confirm(request);
    }
}
