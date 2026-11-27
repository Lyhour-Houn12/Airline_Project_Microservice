package com.jing.seat.repository;

import com.jing.seat.model.FlightInstanceCabin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface FlightInstanceCabinRepository extends JpaRepository<FlightInstanceCabin, Long>, JpaSpecificationExecutor<FlightInstanceCabin> {

    @EntityGraph(attributePaths = "cabinClass")
    List<FlightInstanceCabin> findByFlightInstanceIdOrderByCabinClassDisplayOrderAsc(Long flightInstanceId);


    @Override
    @EntityGraph(attributePaths = "cabinClass")
    Page<FlightInstanceCabin> findAll(Specification<FlightInstanceCabin> spec, Pageable pageable);

    List<FlightInstanceCabin> findByFlightInstanceId(Long flightInstanceId);


}
