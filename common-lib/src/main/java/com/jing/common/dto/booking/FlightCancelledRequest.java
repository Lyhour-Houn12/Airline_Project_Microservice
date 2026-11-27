package com.jing.common.dto.booking;

import jakarta.validation.constraints.Size;

/** The airline cancelled a flight instance (internal: flight-ops-service -> booking-service). */
public record FlightCancelledRequest(
        String flightNumber,
        @Size(max = 200) String reason
) {
}
