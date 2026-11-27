package com.jing.ancillary.mapper;

import com.jing.ancillary.model.Meal;
import com.jing.common.dto.ancillary.MealRequest;
import com.jing.common.dto.ancillary.MealResponse;

public final class MealMapper {
    private MealMapper() {}

    public static Meal toEntity(MealRequest r) {
        return Meal.builder()
                .airlineId(r.airlineId())
                .code(r.code().trim().toUpperCase())
                .name(r.name().trim())
                .description(clean(r.description()))
                .mealType(r.mealType())
                .dietaryType(r.dietaryType())
                .cuisine(clean(r.cuisine()))
                .calories(r.calories())
                .allergens(clean(r.allergens()))
                .basePrice(r.basePrice())
                .currency(r.currency().trim().toUpperCase())
                .isActive(r.isActive() == null ? Boolean.TRUE : r.isActive())
                .build();
    }

    // airlineId, code and the image are NOT touched here
    public static void updateEntity(Meal e, MealRequest r) {
        e.setName(r.name().trim());
        e.setDescription(clean(r.description()));
        e.setMealType(r.mealType());
        e.setDietaryType(r.dietaryType());
        e.setCuisine(clean(r.cuisine()));
        e.setCalories(r.calories());
        e.setAllergens(clean(r.allergens()));
        e.setBasePrice(r.basePrice());
        e.setCurrency(r.currency().trim().toUpperCase());
        if (r.isActive() != null) {
            e.setIsActive(r.isActive());
        }
    }

    public static MealResponse toResponse(Meal e) {
        return new MealResponse(
                e.getId(),
                e.getAirlineId(),
                e.getCode(),
                e.getName(),
                e.getDescription(),
                e.getMealType(),
                e.getDietaryType(),
                e.getCuisine(),
                e.getCalories(),
                e.getAllergens(),
                e.getImageUrl(),
                e.getBasePrice(),
                e.getCurrency(),
                e.getIsActive(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }

    private static String clean(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

}
