package com.jing.common.dto.ancillary;

import com.jing.common.enumeration.CoverageType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InsuranceCoverageResponse(
        Long id,
        Long airlineId,
        String code,
        String name,
        String description,
        String provider,
        CoverageType coverageType,
        BigDecimal coverageAmount,
        BigDecimal premium,
        String currency,
        Integer minAge,
        Integer maxAge,
        String termsUrl,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}