package com.jing.booking.payload.response;

import com.jing.common.enumeration.CabinClassType;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record BookingSegmentResponse(
        Long id,
        Long bookingId,
        int segmentOrder,
        int journeyNumber,
        Long flightId,
        Long flightInstanceId,
        Long airlineId,
        Long fareId,
        Long cabinClassId,
        CabinClassType cabinClass,
        Long departureAirportId,
        Long arrivalAirportId,
        LocalDateTime departureTime,
        LocalDateTime arrivalTime,
        Instant departureAt,          // UTC
        Instant arrivalAt,            // UTC
        String departureTimeZone,
        String arrivalTimeZone,
        String flightNumber,
        List<PassengerSegmentResponse> passengerSegments
) {
}
