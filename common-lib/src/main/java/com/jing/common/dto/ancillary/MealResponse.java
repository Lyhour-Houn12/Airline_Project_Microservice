package com.jing.common.dto.ancillary;

import com.jing.common.enumeration.DietaryType;
import com.jing.common.enumeration.MealType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MealResponse(
        Long id,
        Long airlineId,
        String code,
        String name,
        String description,
        MealType mealType,
        DietaryType dietaryType,
        String cuisine,
        Integer calories,
        String allergens,
        String imageUrl,
        BigDecimal basePrice,
        String currency,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
