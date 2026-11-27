package com.jing.seat.repository;

import com.jing.common.enumeration.CabinClassType;
import com.jing.seat.model.CabinClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface CabinClassRepository extends JpaRepository<CabinClass, Long>, JpaSpecificationExecutor<CabinClass> {

    boolean existsByAircraftIdAndCodeIgnoreCase(
            Long aircraftId,
            String code
    );

    boolean existsByAircraftIdAndCodeIgnoreCaseAndIdNot(
            Long aircraftId,
            String code,
            Long id
    );

     // for update, if you let the cabin change

    List<CabinClass> findByAircraftIdAndCabinClassType(
            Long aircraftId,
            CabinClassType cabinClassType
    );
}