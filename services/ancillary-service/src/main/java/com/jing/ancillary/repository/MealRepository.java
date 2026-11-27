package com.jing.ancillary.repository;

import com.jing.ancillary.model.Meal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MealRepository extends JpaRepository<Meal, Long>, JpaSpecificationExecutor<Meal> {
    boolean existsByAirlineIdAndCode(Long airlineId, String code);
}
