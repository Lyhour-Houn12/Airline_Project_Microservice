package com.jing.pricing.payload.filter;

import com.jing.common.enumeration.CabinClassType;

import java.math.BigDecimal;

public record FareFilter(
        Long airlineId,
        Long flightId,
        Long cabinClassId,
        CabinClassType cabinClass,
        String currency,
        BigDecimal minPrice,
        BigDecimal maxPrice
) {

}