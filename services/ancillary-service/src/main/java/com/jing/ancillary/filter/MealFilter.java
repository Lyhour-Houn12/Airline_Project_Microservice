package com.jing.ancillary.filter;

import com.jing.common.enumeration.DietaryType;
import com.jing.common.enumeration.MealType;

import java.math.BigDecimal;

public record MealFilter(
        Long airlineId,
        MealType mealType,
        DietaryType dietaryType,
        String cuisine,
        String currency,
        Boolean isActive,
        String keyword,
        Integer maxCalories,
        BigDecimal minPrice,
        BigDecimal maxPrice
) {
}
