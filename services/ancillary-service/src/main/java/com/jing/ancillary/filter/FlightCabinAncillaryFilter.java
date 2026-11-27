package com.jing.ancillary.filter;

import com.jing.common.enumeration.AncillaryType;

public record FlightCabinAncillaryFilter(
        Long flightInstanceCabinId,
        Long ancillaryId,
        AncillaryType ancillaryType,
        Boolean isFree,
        Boolean isActive,
        Boolean inStock,       // true = unlimited, or remaining quantity > 0
        boolean publicView     // true = entry active AND ancillary active (customers)
) {}