package com.jing.seat.filter;

import com.jing.common.enumeration.CabinClassType;

public record FlightInstanceCabinFilter(
        Long flightInstanceId,
        Long cabinClassId,
        Long aircraftId,
        CabinClassType cabinClassType,
        Boolean isBookable,
        Boolean hasAvailability
) {}