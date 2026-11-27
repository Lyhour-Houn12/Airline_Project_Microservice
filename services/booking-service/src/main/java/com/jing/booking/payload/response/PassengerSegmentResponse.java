package com.jing.booking.payload.response;

import com.jing.common.enumeration.PassengerSegmentStatus;
import com.jing.common.enumeration.PassengerType;

import java.math.BigDecimal;

public record PassengerSegmentResponse(
        Long id,
        Long segmentId,
        Long passengerId,
        String passengerName,
        PassengerType passengerType,
        Long seatInstanceId,
        PassengerSegmentStatus status,
        BigDecimal baseFare,
        BigDecimal taxesAndFees,
        BigDecimal airlineFees,
        BigDecimal seatSurcharge,
        BigDecimal totalPrices
) {
}
