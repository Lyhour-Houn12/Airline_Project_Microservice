package com.jing.common.dto.seat;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Hold one seat instance for a booking (internal: booking-service -> seat-service).
 *
 * @param flightInstanceId the flight the booking segment is on: the seat must belong to it
 * @param cabinClassId     the cabin the passenger paid for: the seat must be in it
 * @param holdReference    who holds the seat, i.e. the booking reference (PNR)
 * @param holdUntil        when the hold lapses if the booking is never paid
 */
public record SeatHoldRequest(
        @NotNull Long flightInstanceId,
        @NotNull Long cabinClassId,
        @NotBlank @Size(max = 20) String holdReference,
        @NotNull @Future LocalDateTime holdUntil
) {
}
