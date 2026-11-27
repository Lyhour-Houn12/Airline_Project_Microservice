package com.jing.common.dto.seat;

import com.jing.common.enumeration.SeatAvailabilityStatus;
import com.jing.common.enumeration.SeatType;

import java.math.BigDecimal;

public record SeatInstanceResponse(
        Long id,
        Long flightInstanceId,
        Long flightInstanceCabinId,
        String cabinClassName,
        Long seatId,
        String seatNumber,
        SeatType seatType,
        SeatAvailabilityStatus status,
        BigDecimal surcharge,
        String blockedReason
        ) {
}
