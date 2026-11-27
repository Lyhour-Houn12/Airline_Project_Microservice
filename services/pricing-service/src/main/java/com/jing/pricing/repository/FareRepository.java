package com.jing.pricing.repository;

import com.jing.pricing.model.Fare;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FareRepository extends JpaRepository<Fare, Long>, JpaSpecificationExecutor<Fare> {
    boolean existsByFlightIdAndCabinClassIdAndFareName(Long flightId, Long cabinClassId, String fareName);

    boolean existsByFlightIdAndCabinClassIdAndFareNameAndIdNot(
            Long flightId, Long cabinClassId, String fareName, Long id);

    // loads the one-to-one children in the same query, so listing a page doesn't trigger N+1
    @Override
    @EntityGraph(attributePaths = {"fareRule", "baggagePolicy"})
    Page<Fare> findAll(Specification<Fare> spec, Pageable pageable);
}
