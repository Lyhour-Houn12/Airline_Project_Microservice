package com.jing.ancillary.repository;

import com.jing.ancillary.model.FlightInstanceMeal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FlightInstanceMealRepository extends JpaRepository<FlightInstanceMeal, Long>, JpaSpecificationExecutor<FlightInstanceMeal> {

    boolean existsByFlightInstanceIdAndMealId(Long flightInstanceId, Long mealId);

    boolean existsByMealId(Long mealId);


    @Override
    @EntityGraph(attributePaths = "meal")
    Page<FlightInstanceMeal> findAll(Specification<FlightInstanceMeal> spec, Pageable pageable);

}
