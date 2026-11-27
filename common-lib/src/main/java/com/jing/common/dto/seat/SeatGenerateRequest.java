package com.jing.common.dto.seat;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SeatGenerateRequest(
        @NotNull Long seatMapId,
        @NotNull @Min(1) @Max(100) Integer startRow
) {}