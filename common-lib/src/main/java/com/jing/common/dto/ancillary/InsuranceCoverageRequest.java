package com.jing.common.dto.ancillary;

import com.jing.common.enumeration.CoverageType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record InsuranceCoverageRequest(

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

        @NotBlank(message = "provider is required")
        @Size(max = 150, message = "provider must not exceed 150 characters")
        String provider,

        @NotNull(message = "coverageType is required")
        CoverageType coverageType,

        @NotNull(message = "coverageAmount is required")
        @DecimalMin(value = "0.01", message = "coverageAmount must be greater than 0")
        @Digits(integer = 10, fraction = 2, message = "coverageAmount allows up to 10 integer and 2 decimal digits")
        BigDecimal coverageAmount,

        @NotNull(message = "premium is required")
        @DecimalMin(value = "0.00", message = "premium must be >= 0")
        @Digits(integer = 10, fraction = 2, message = "premium allows up to 10 integer and 2 decimal digits")
        BigDecimal premium,

        @NotBlank(message = "currency is required")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "currency must be a 3-letter ISO code")
        String currency,

        @Min(value = 0, message = "minAge must be >= 0")
        @Max(value = 120, message = "minAge must be <= 120")
        Integer minAge,

        @Min(value = 0, message = "maxAge must be >= 0")
        @Max(value = 120, message = "maxAge must be <= 120")
        Integer maxAge,

        @Size(max = 500, message = "termsUrl must not exceed 500 characters")
        @Pattern(regexp = "^(https?://.+)?$", message = "termsUrl must start with http:// or https://")
        String termsUrl,

        Boolean isActive
) {}