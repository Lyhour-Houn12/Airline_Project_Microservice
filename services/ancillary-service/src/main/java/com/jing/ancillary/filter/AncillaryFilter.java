package com.jing.ancillary.filter;

import com.jing.common.enumeration.AncillaryType;

import java.math.BigDecimal;

public record AncillaryFilter(
        Long airlineId,
        AncillaryType type,
        String currency,
        Boolean isActive,
        String keyword,
        BigDecimal minPrice,
        BigDecimal maxPrice
) {
}
