package com.jing.common.dto.ancillary;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record FlightInstanceMealUpdateRequest(

        @NotNull(message = "price is required")
        @DecimalMin(value = "0.00", message = "price must be >= 0")
        @Digits(integer = 10, fraction = 2, message = "price allows up to 10 integer and 2 decimal digits")
        BigDecimal price,

        @NotNull(message = "totalQuantity is required")
        @Min(value = 1, message = "totalQuantity must be at least 1")
        @Max(value = 100000, message = "totalQuantity must be <= 100000")
        Integer totalQuantity,

        @NotNull(message = "orderCutoffHours is required")
        @Min(value = 0, message = "orderCutoffHours must be >= 0")
        @Max(value = 720, message = "orderCutoffHours must be <= 720")
        Integer orderCutoffHours,

        Boolean isActive
) {}