package com.jing.common.dto.ancillary;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record FlightCabinAncillaryUpdateRequest(

        // required when isFree = false; must be 0 or omitted when isFree = true
        @DecimalMin(value = "0.00", message = "price must be >= 0")
        @Digits(integer = 10, fraction = 2, message = "price allows up to 10 integer and 2 decimal digits")
        BigDecimal price,

        @NotNull(message = "isFree is required")
        Boolean isFree,

        // null = unlimited
        @Min(value = 1, message = "totalQuantity must be at least 1")
        @Max(value = 100000, message = "totalQuantity must be <= 100000")
        Integer totalQuantity,

        Boolean isActive
) {}