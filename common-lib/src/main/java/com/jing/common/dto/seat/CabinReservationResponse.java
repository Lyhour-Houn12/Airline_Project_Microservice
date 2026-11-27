package com.jing.common.dto.seat;

/** The booking's places in the cabin after the change, and how many are still free. */
public record CabinReservationResponse(
        Long flightInstanceCabinId,
        int seats,
        int availableSeats
) {
}
