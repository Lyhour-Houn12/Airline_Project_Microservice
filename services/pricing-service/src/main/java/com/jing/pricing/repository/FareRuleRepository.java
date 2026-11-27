package com.jing.pricing.repository;

import com.jing.pricing.model.FareRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FareRuleRepository extends JpaRepository<FareRule, Long> {

    boolean existsByFareId(Long fareId);

    Optional<FareRule> findByFareId(Long fareId);
}