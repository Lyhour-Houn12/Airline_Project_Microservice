package com.jing.booking.payload.response;

import com.jing.common.enumeration.CabinClassType;

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
        String flightNumber,
        List<PassengerSegmentResponse> passengerSegments
) {
}
