package com.jing.common.dto.ancillary;

import com.jing.common.enumeration.DietaryType;
import com.jing.common.enumeration.MealType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;


public record MealRequest(

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

        @NotNull(message = "mealType is required")
        MealType mealType,

        @NotNull(message = "dietaryType is required")
        DietaryType dietaryType,

        @Size(max = 100, message = "cuisine must not exceed 100 characters")
        String cuisine,

        @Min(value = 0, message = "calories must be >= 0")
        @Max(value = 5000, message = "calories must be <= 5000")
        Integer calories,

        @Size(max = 255, message = "allergens must not exceed 255 characters")
        String allergens,

        @NotNull(message = "basePrice is required")
        @DecimalMin(value = "0.00", message = "basePrice must be >= 0")
        @Digits(integer = 10, fraction = 2, message = "basePrice allows up to 10 integer and 2 decimal digits")
        BigDecimal basePrice,

        @NotBlank(message = "currency is required")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "currency must be a 3-letter ISO code")
        String currency,

        Boolean isActive
) {}