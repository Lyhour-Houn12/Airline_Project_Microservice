package com.jing.flight.repository;

import com.jing.flight.model.FlightInstance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface FlightInstanceRepository extends JpaRepository<FlightInstance, Long>, JpaSpecificationExecutor<FlightInstance> {
    boolean existsByFlightIdAndDepartureDateTime(Long flightId, LocalDateTime departureDateTime);

    // Avoids N+1 on the lazy Flight (the mapper reads flight number and aircraft id)
    @Override
    @EntityGraph(attributePaths = "flight")
    Page<FlightInstance> findAll(Specification<FlightInstance> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "flight")
    List<FlightInstance> findAllById(Iterable<Long> ids);

    boolean existsByScheduleId(Long scheduleId);

    @Query("select fi.departureDateTime from FlightInstance fi " +
            "where fi.flight.id = :flightId and fi.departureDateTime >= :start and fi.departureDateTime < :end")
    List<LocalDateTime> findDepartureTimes(@Param("flightId") Long flightId,
                                           @Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end);

}
