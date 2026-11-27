package com.jing.common.dto.ancillary;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record FlightInstanceMealBulkRequest(

        @NotNull(message = "flightInstanceId is required")
        @Positive(message = "flightInstanceId must be positive")
        Long flightInstanceId,

        @NotEmpty(message = "meals must not be empty")
        @Size(max = 50, message = "meals must not exceed 50 items")
        List<@Valid @NotNull(message = "meal item must not be null") FlightInstanceMealItemRequest> meals
) {}