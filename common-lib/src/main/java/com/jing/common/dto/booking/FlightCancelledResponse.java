package com.jing.common.dto.booking;

/** How many bookings on the cancelled flight were cancelled now (0 when repeated). */
public record FlightCancelledResponse(Long flightInstanceId, int bookingsCancelled) {
}
