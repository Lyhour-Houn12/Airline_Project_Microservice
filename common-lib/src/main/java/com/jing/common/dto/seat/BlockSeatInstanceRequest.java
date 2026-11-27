package com.jing.common.dto.seat;

import jakarta.validation.constraints.NotBlank;

public record BlockSeatInstanceRequest(
        @NotBlank(message = "reason is required") String reason) {}