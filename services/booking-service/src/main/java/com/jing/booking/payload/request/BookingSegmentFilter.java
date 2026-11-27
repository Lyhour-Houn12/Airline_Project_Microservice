package com.jing.booking.payload.request;

import com.jing.common.enumeration.CabinClassType;

import java.time.LocalDateTime;

public record BookingSegmentFilter(
        Long flightId, Long flightInstanceId, Long airlineId, CabinClassType cabinClass,
        Long departureAirportId, Long arrivalAirportId,
        LocalDateTime departureFrom, LocalDateTime departureTo
) {}