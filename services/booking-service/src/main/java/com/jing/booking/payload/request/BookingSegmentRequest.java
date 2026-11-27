package com.jing.booking.payload.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Add a flight to a booking. Only ids are accepted: airports, times, flight number, cabin and price
 * are read from flight-ops-service and pricing-service, so the client can't make them up.
 *
 * @param journeyNumber optional; omit for the current journey, or pass the next number to start
 *                      the return (round trip) or the next city (multi-city)
 */
public record BookingSegmentRequest(
        @NotNull @Positive Long flightInstanceId,
        @NotNull @Positive Long fareId,
        @Min(1) Integer journeyNumber
) {
}
