package com.jing.location.repository;

import com.jing.location.model.Airport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AirportRepository extends JpaRepository<Airport, Long> {
    Optional<Airport> findByIataCode(String iataCode);
    boolean existsByIataCodeIgnoreCase(String iataCode);
    boolean existsByIataCodeIgnoreCaseAndIdNot(String iataCode, Long id);
    boolean existsByCityId(Long cityId);

    List<Airport> findByCityId(Long cityId);

    @Query("""
        SELECT a FROM Airport a
        JOIN a.city c
        WHERE (:keyword IS NULL
            OR LOWER(a.airportName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(a.iataCode)    LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.cityName)    LIKE LOWER(CONCAT('%', :keyword, '%')))
    """)
    Page<Airport> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
