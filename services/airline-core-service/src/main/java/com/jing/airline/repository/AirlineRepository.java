package com.jing.airline.repository;

import com.jing.airline.model.Airline;
import com.jing.common.enumeration.AirlineStatus;
import com.jing.common.dto.airline.AirlineDropDownItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AirlineRepository extends JpaRepository<Airline, Long> {
    Optional<Airline> findByOwnerId(Long ownerId);

    boolean existsByOwnerId(Long ownerId);
    boolean existsByIataCodeIgnoreCase(String iataCode);
    boolean existsByIcaoCodeIgnoreCase(String icaoCode);
    boolean existsByIataCodeIgnoreCaseAndIdNot(String iataCode, Long id);
    boolean existsByIcaoCodeIgnoreCaseAndIdNot(String icaoCode, Long id);


    @Query("""
        select a from Airline a
        where lower(a.airlineName) like lower(concat('%', :keyword, '%'))
           or lower(a.iataCode)    like lower(concat('%', :keyword, '%'))
           or lower(a.icaoCode)    like lower(concat('%', :keyword, '%'))
           or lower(a.country)     like lower(concat('%', :keyword, '%'))
        """)
    Page<Airline> search(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
    select new com.jing.common.dto.airline.AirlineDropDownItem(
        a.id,
        a.airlineName,
        a.iataCode,
        a.icaoCode,
        a.logoUrl,
        a.country)
    from Airline a
    where a.status = :status
    order by a.airlineName
    """)
    List<AirlineDropDownItem> findDropdownItems(@Param("status") AirlineStatus status);
}
