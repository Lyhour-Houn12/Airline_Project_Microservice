package com.jing.common.dto.seat;

import com.jing.common.enumeration.SeatType;

import java.time.LocalDateTime;

public record SeatResponse(
        Long id,
        Long aircraftId,
        Long seatMapId,
        Long cabinClassId,
        String seatNumber,
        Integer seatRow,
        Character columnLetter,
        SeatType seatType,
        Boolean isAvailable,
        Boolean isBlocked,
        Boolean isActive,
        Boolean isEmergencyExit,
        Boolean hasExtraRoom,
        Boolean hasBassinet,
        Boolean isNearLavatory,
        Boolean isNearGalley,
        Boolean hasPowerOutlet,
        Boolean hasTvScreen,
        Boolean isWheelchairAccessible,
        Boolean hasExtraWidth,
        Integer seatPitch,
        Integer seatWidth,
        Integer reclineAngle,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}