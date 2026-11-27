package com.jing.seat.repository.projection;

import com.jing.common.enumeration.SeatAvailabilityStatus;

public record CabinStatusCount(
        Long flightInstanceCabinId,
        SeatAvailabilityStatus status,
        long count
) {
}
