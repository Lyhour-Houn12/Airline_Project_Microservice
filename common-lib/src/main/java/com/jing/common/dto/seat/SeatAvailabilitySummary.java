package com.jing.common.dto.seat;

import java.util.List;

public record SeatAvailabilitySummary(
        Long flightInstanceId,
        StatusCounts total,
        List<CabinSummary> cabins
) {

    public record CabinSummary(Long flightInstanceCabinId, String cabinClassName, StatusCounts counts) {}
    public record StatusCounts(long total, long available, long held, long booked, long blocked) {}
}
