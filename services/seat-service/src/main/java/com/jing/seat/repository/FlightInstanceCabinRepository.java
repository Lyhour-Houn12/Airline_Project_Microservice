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



    /** Row lock: capacity decisions on one cabin happen one at a time (no two bookings get the last seat). */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("""
        SELECT c FROM FlightInstanceCabin c JOIN FETCH c.cabinClass cc
        WHERE c.flightInstanceId = :flightInstanceId AND cc.id = :cabinClassId
        """)
    java.util.Optional<FlightInstanceCabin> lockByFlightInstanceAndCabinClass(
            @org.springframework.data.repository.query.Param("flightInstanceId") Long flightInstanceId,
            @org.springframework.data.repository.query.Param("cabinClassId") Long cabinClassId);

    /** Row lock by id. */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT c FROM FlightInstanceCabin c WHERE c.id = :id")
    java.util.Optional<FlightInstanceCabin> lockById(@org.springframework.data.repository.query.Param("id") Long id);
}
