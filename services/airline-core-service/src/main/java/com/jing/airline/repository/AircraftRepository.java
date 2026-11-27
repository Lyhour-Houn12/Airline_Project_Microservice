package com.jing.airline.repository;

import com.jing.airline.model.Aircraft;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AircraftRepository extends JpaRepository<Aircraft, Long> {
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
    boolean existsByAirlineId(Long airlineId);

    // finds by id AND owner in one query; an aircraft of another owner is simply "not found"
    @EntityGraph(attributePaths = "airline")
    Optional<Aircraft> findByIdAndAirlineOwnerId(Long id, Long ownerId);

    @EntityGraph(attributePaths = "airline")
    List<Aircraft> findAllByAirlineOwnerIdOrderByCodeAsc(Long ownerId);

    @EntityGraph(attributePaths = "airline")
    @Query("""
        select a from Aircraft a
        join a.airline al
        where lower(a.code)         like lower(concat('%', :keyword, '%'))
           or lower(a.model)        like lower(concat('%', :keyword, '%'))
           or lower(a.manufacturer) like lower(concat('%', :keyword, '%'))
           or lower(al.airlineName) like lower(concat('%', :keyword, '%'))
        """)
    Page<Aircraft> search(@Param("keyword") String keyword, Pageable pageable);
}
