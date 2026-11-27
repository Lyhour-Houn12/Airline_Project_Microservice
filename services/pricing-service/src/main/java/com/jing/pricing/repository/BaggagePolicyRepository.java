package com.jing.pricing.repository;

import com.jing.pricing.model.BaggagePolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BaggagePolicyRepository extends JpaRepository<BaggagePolicy, Long> {
    boolean existsByFareId(Long fareId);

    Optional<BaggagePolicy> findByFareId(Long fareId);
}
