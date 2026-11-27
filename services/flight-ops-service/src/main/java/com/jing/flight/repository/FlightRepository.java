package com.jing.flight.repository;

import com.jing.flight.model.Flight;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FlightRepository extends JpaRepository<Flight, Long>, JpaSpecificationExecutor<Flight> {

    Optional<Flight> findByFlightNumber(String flightNumber);
    boolean existsByFlightNumber(String flightNumber);
    boolean existsByFlightNumberAndIdNot(String flightNumber, Long id);

    @Query("select f.flightNumber from Flight f where f.flightNumber in :numbers")
    List<String> findExistingFlightNumbers(@Param("numbers") Collection<String> numbers);

}
