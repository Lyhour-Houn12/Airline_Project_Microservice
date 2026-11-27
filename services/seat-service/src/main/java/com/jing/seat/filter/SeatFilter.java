package com.jing.seat.filter;

import com.jing.common.enumeration.SeatType;

public record SeatFilter(
        Long seatMapId,
        Long aircraftId,
        SeatType seatType,
        Integer seatRow,
        Character columnLetter,
        Boolean isAvailable,
        Boolean isBlocked,
        Boolean isActive,
        Boolean isEmergencyExit,
        Boolean isWheelchairAccessible
) {}