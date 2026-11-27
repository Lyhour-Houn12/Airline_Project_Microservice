package com.jing.common.dto.ancillary;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record FlightInstanceMealItemRequest(

        @NotNull(message = "mealId is required")
        @Positive(message = "mealId must be positive")
        Long mealId,

        // optional: defaults to the meal's basePrice
        @DecimalMin(value = "0.00", message = "price must be >= 0")
        @Digits(integer = 10, fraction = 2, message = "price allows up to 10 integer and 2 decimal digits")
        BigDecimal price,

        @NotNull(message = "totalQuantity is required")
        @Min(value = 1, message = "totalQuantity must be at least 1")
        @Max(value = 100000, message = "totalQuantity must be <= 100000")
        Integer totalQuantity,

        // optional: defaults to 24
        @Min(value = 0, message = "orderCutoffHours must be >= 0")
        @Max(value = 720, message = "orderCutoffHours must be <= 720")
        Integer orderCutoffHours,

        Boolean isActive
) {}