package com.jing.common.dto.ancillary;

import com.jing.common.enumeration.AncillaryType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FlightCabinAncillaryResponse(
        Long id,
        Long flightInstanceCabinId,
        Long ancillaryId,
        String ancillaryCode,
        String ancillaryName,
        AncillaryType ancillaryType,
        BigDecimal price,
        String currency,
        Boolean isFree,
        Integer totalQuantity,       // null = unlimited
        Integer soldQuantity,
        Integer remainingQuantity,   // null = unlimited
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}