package com.jing.booking.payload.request;

import com.jing.common.enumeration.CabinClassType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record BookingSegmentRequest(
        @NotNull Long flightId,
        @NotNull Long flightInstanceId,
        @NotNull Long airlineId,
        @NotNull Long fareId,
        @NotNull Long cabinClassId,
        @NotNull CabinClassType cabinClass,
        @NotNull Long departureAirportId,
        @NotNull Long arrivalAirportId,
        @NotNull LocalDateTime departureTime,
        @NotNull LocalDateTime arrivalTime,
        @Size(max = 10) String flightNumber,
        @Min(1) Integer journeyNumber
) {
}
