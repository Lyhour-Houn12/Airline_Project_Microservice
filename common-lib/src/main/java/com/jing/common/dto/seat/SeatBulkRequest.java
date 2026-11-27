package com.jing.common.dto.seat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SeatBulkRequest(
        @NotEmpty @Size(max = 300) List<@Valid SeatRequest> seats
) {}