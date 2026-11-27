package com.jing.common.dto.ancillary;

import com.jing.common.enumeration.AncillaryType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AncillaryResponse(
        Long id,
        Long airlineId,
        String code,
        String name,
        String description,
        AncillaryType type,
        BigDecimal basePrice,
        String currency,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}