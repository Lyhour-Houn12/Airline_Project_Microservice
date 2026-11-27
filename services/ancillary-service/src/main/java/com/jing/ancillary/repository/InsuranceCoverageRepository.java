package com.jing.ancillary.repository;

import com.jing.ancillary.model.InsuranceCoverage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface InsuranceCoverageRepository
        extends JpaRepository<InsuranceCoverage, Long>, JpaSpecificationExecutor<InsuranceCoverage> {

    boolean existsByAirlineIdAndCode(Long airlineId, String code);
}