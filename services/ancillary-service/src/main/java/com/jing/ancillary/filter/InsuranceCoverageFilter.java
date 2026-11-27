package com.jing.ancillary.filter;

import com.jing.common.enumeration.CoverageType;

import java.math.BigDecimal;

public record InsuranceCoverageFilter(
        Long airlineId,
        CoverageType coverageType,
        String provider,
        String currency,
        Boolean isActive,
        String keyword,
        Integer eligibleAge,     // only coverages a person of this age can buy
        BigDecimal minPremium,
        BigDecimal maxPremium
) {}