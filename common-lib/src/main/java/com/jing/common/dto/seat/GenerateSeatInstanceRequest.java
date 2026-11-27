package com.jing.common.dto.seat;

import jakarta.validation.constraints.NotNull;

public record GenerateSeatInstanceRequest(
        @NotNull(message = "Flight instance ID is required") Long  flightInstanceId
) {
}
