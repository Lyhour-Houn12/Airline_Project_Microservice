package com.jing.ancillary.specification;

import com.jing.ancillary.filter.FlightInstanceMealFilter;
import com.jing.ancillary.model.FlightInstanceMeal;
import com.jing.ancillary.model.Meal;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class FlightInstanceMealSpecification {

    private FlightInstanceMealSpecification() {
        // Private constructor to prevent instantiation
    }

    public static Specification<FlightInstanceMeal> withFilter(FlightInstanceMealFilter f) {
        // Implementation for creating specification based on filter
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            Join<FlightInstanceMeal, Meal> meal = root.join("meal");

            if (f.flightInstanceId() != null) {
                p.add(cb.equal(root.get("flightInstanceId"), f.flightInstanceId()));
            }
            if (f.mealId() != null) {
                p.add(cb.equal(meal.get("id"), f.mealId()));
            }
            if (f.mealType() != null) {
                p.add(cb.equal(meal.get("mealType"), f.mealType()));
            }
            if (f.dietaryType() != null) {
                p.add(cb.equal(meal.get("dietaryType"), f.dietaryType()));
            }

            if (f.isActive() != null) {
                p.add(cb.equal(root.get("isActive"), f.isActive()));
            }
            if (Boolean.TRUE.equals(f.inStock())) {
                p.add(cb.greaterThan(
                        cb.diff(root.<Integer>get("totalQuantity"), root.<Integer>get("soldQuantity")), 0));
            }
            if (f.publicView()) {
                p.add(cb.isTrue(root.get("isActive")));
                p.add(cb.isTrue(meal.get("isActive")));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

}
