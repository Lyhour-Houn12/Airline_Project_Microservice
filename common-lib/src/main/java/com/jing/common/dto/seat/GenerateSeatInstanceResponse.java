package com.jing.common.dto.seat;

import java.util.List;

public record GenerateSeatInstanceResponse(
        Long flightInstanceId,
        int totalCreated,
        List<CabinGenerated> cabins
) {
    public record CabinGenerated(Long flightInstanceCabinId, String cabinClassName, int created) {}
}
