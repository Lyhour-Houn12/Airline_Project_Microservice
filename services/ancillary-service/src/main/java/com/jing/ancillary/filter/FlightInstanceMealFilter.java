package com.jing.ancillary.filter;

import com.jing.common.enumeration.DietaryType;
import com.jing.common.enumeration.MealType;

public record FlightInstanceMealFilter(
        Long flightInstanceId,
        Long mealId,
        MealType mealType,
        DietaryType dietaryType,
        Boolean isActive,
        Boolean inStock,       // true = only entries with remaining quantity > 0
        boolean publicView     // true = entry active AND meal active (customers)
) {}