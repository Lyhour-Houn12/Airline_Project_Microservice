package com.jing.common.dto.ancillary;

import com.jing.common.enumeration.DietaryType;
import com.jing.common.enumeration.MealType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FlightInstanceMealResponse(
        Long id,
        Long flightInstanceId,
        Long mealId,
        String mealCode,
        String mealName,
        MealType mealType,
        DietaryType dietaryType,
        String imageUrl,
        BigDecimal price,
        String currency,
        Integer totalQuantity,
        Integer soldQuantity,
        Integer remainingQuantity,
        Integer orderCutoffHours,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}