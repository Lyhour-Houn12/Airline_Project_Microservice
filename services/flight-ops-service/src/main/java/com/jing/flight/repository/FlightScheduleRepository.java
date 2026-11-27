package com.jing.flight.repository;

import com.jing.flight.model.FlightSchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FlightScheduleRepository extends JpaRepository<FlightSchedule, Long>, JpaSpecificationExecutor<FlightSchedule> {

    @Override
    @EntityGraph(attributePaths = "flight")
    Page<FlightSchedule> findAll(Specification<FlightSchedule> spec, Pageable pageable);
}
