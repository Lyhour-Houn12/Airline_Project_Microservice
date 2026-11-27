package com.jing.common.dto.seat;

import com.jing.common.enumeration.CabinClassType;

import java.time.LocalDateTime;

public record FlightInstanceCabinResponse(
        Long id,
        Long flightInstanceId,
        Long cabinClassId,
        String cabinClassName,
        String cabinClassCode,
        CabinClassType cabinClassType,
        Long aircraftId,
        Integer totalSeats,
        Integer bookedSeats,
        Integer availableSeats,
        Boolean isBookable,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
