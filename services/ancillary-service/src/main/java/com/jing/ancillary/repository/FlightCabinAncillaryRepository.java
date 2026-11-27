package com.jing.ancillary.repository;

import com.jing.ancillary.model.FlightCabinAncillary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FlightCabinAncillaryRepository
        extends JpaRepository<FlightCabinAncillary, Long>, JpaSpecificationExecutor<FlightCabinAncillary> {

    boolean existsByFlightInstanceCabinIdAndAncillaryId(Long flightInstanceCabinId, Long ancillaryId);

    boolean existsByAncillaryId(Long ancillaryId);

    // load the ancillary together with the page, so mapping does not fire one query per row
    @Override
    @EntityGraph(attributePaths = "ancillary")
    Page<FlightCabinAncillary> findAll(Specification<FlightCabinAncillary> spec, Pageable pageable);
}