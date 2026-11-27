package com.jing.seat.repository;

import com.jing.seat.model.SeatMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface SeatMapRepository extends JpaRepository<SeatMap, Long> , JpaSpecificationExecutor<SeatMap> {

    Optional<SeatMap> findFirstByCabinClass_IdOrderByIdAsc(Long cabinId);

    boolean existsByNameIgnoreCaseAndAirlineIdAndCabinClass_Id(
            String name,
            Long airlineId,
            Long cabinClassId
    );

    boolean existsByCabinClassId(Long cabinClassId);
    boolean existsByCabinClassIdAndIdNot(Long cabinClassId, Long id);

    boolean existsByNameIgnoreCaseAndAirlineIdAndCabinClass_IdAndIdNot(String name, Long airlineId, Long cabinClassId, Long id);
}
