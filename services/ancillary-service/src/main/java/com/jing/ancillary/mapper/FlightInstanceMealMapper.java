package com.jing.ancillary.mapper;

import com.jing.ancillary.model.FlightInstanceMeal;
import com.jing.ancillary.model.Meal;
import com.jing.common.dto.ancillary.FlightInstanceMealResponse;

public final class FlightInstanceMealMapper {

    public static final int DEFAULT_CUTOFF_HOURS = 24;

    private FlightInstanceMealMapper() {}

    public static FlightInstanceMeal toEntity(Long flightInstanceId,
                                              com.jing.common.dto.ancillary.FlightInstanceMealItemRequest r,
                                              Meal meal) {
        return FlightInstanceMeal.builder()
                .flightInstanceId(flightInstanceId)
                .meal(meal)
                .price(r.price() != null ? r.price() : meal.getBasePrice())
                .currency(meal.getCurrency())
                .totalQuantity(r.totalQuantity())
                .soldQuantity(0)
                .orderCutoffHours(r.orderCutoffHours() != null
                        ? r.orderCutoffHours() : DEFAULT_CUTOFF_HOURS)
                .isActive(r.isActive() == null ? Boolean.TRUE : r.isActive())
                .build();
    }

    // flightInstanceId and meal are immutable, so they are not touched here
    public static void updateEntity(FlightInstanceMeal e, com.jing.common.dto.ancillary.FlightInstanceMealUpdateRequest r) {
        e.setPrice(r.price());
        e.setTotalQuantity(r.totalQuantity());
        e.setOrderCutoffHours(r.orderCutoffHours());
        if (r.isActive() != null) {
            e.setIsActive(r.isActive());
        }
    }

    public static FlightInstanceMealResponse toResponse(FlightInstanceMeal e) {
        Meal m = e.getMeal();
        return new FlightInstanceMealResponse(
                e.getId(),
                e.getFlightInstanceId(),
                m.getId(),
                m.getCode(),
                m.getName(),
                m.getMealType(),
                m.getDietaryType(),
                m.getImageUrl(),
                e.getPrice(),
                e.getCurrency(),
                e.getTotalQuantity(),
                e.getSoldQuantity(),
                e.getTotalQuantity() - e.getSoldQuantity(),
                e.getOrderCutoffHours(),
                e.getIsActive(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}