package com.jing.common.dto.ancillary;

import com.jing.common.enumeration.AncillaryType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record AncillaryRequest(
        @NotNull(message = "airlineId is required")
        @Positive(message = "airlineId must be positive")
        Long airlineId,

        @NotBlank(message = "code is required")
        @Pattern(regexp = "^[A-Za-z0-9_-]{2,30}$",
                message = "code must be 2-30 characters (letters, digits, '_' or '-')")
        String code,

        @NotBlank(message = "name is required")
        @Size(max = 150, message = "name must not exceed 150 characters")
        String name,

        @Size(max = 1000, message = "description must not exceed 1000 characters")
        String description,

        @NotNull(message = "type is required")
        AncillaryType type,

        @NotNull(message = "basePrice is required")
        @DecimalMin(value = "0.00", message = "basePrice must be >= 0")
        @Digits(integer = 10, fraction = 2, message = "basePrice allows up to 10 integer and 2 decimal digits")
        BigDecimal basePrice,

        @NotBlank(message = "currency is required")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "currency must be a 3-letter ISO code")
        String currency,

        Boolean isActive
) {}