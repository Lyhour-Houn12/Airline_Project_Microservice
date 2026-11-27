package com.jing.ancillary.repository;

import com.jing.ancillary.model.Ancillary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AncillaryRepository extends JpaRepository<Ancillary, Long>, JpaSpecificationExecutor<Ancillary> {

    boolean existsByAirlineIdAndCode(Long airlineId, String code);
}
