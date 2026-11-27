package com.jing.common.dto.ancillary;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record FlightCabinAncillaryItemRequest(

        @NotNull(message = "ancillaryId is required")
        @Positive(message = "ancillaryId must be positive")
        Long ancillaryId,

        // optional: defaults to the ancillary's basePrice; must be 0 or omitted when isFree = true
        @DecimalMin(value = "0.00", message = "price must be >= 0")
        @Digits(integer = 10, fraction = 2, message = "price allows up to 10 integer and 2 decimal digits")
        BigDecimal price,

        // optional: defaults to false
        Boolean isFree,

        // optional: null = unlimited
        @Min(value = 1, message = "totalQuantity must be at least 1")
        @Max(value = 100000, message = "totalQuantity must be <= 100000")
        Integer totalQuantity,

        Boolean isActive
) {}