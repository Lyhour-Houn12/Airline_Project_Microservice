package com.jing.ancillary.mapper;

import com.jing.ancillary.model.Ancillary;
import com.jing.ancillary.model.FlightCabinAncillary;
import com.jing.common.dto.ancillary.FlightCabinAncillaryResponse;

import java.math.BigDecimal;

public final class FlightCabinAncillaryMapper {

    private FlightCabinAncillaryMapper() {}

    public static FlightCabinAncillary toEntity(Long flightInstanceCabinId,
                                                com.jing.common.dto.ancillary.FlightCabinAncillaryItemRequest r,
                                                Ancillary ancillary,
                                                BigDecimal resolvedPrice,
                                                boolean isFree) {
        return FlightCabinAncillary.builder()
                .flightInstanceCabinId(flightInstanceCabinId)
                .ancillary(ancillary)
                .price(resolvedPrice)
                .currency(ancillary.getCurrency())
                .isFree(isFree)
                .totalQuantity(r.totalQuantity())
                .soldQuantity(0)
                .isActive(r.isActive() == null ? Boolean.TRUE : r.isActive())
                .build();
    }

    // flightInstanceCabinId and ancillary are immutable, so they are not touched here
    public static void updateEntity(FlightCabinAncillary e,
                                    com.jing.common.dto.ancillary.FlightCabinAncillaryUpdateRequest r,
                                    BigDecimal resolvedPrice) {
        e.setPrice(resolvedPrice);
        e.setIsFree(r.isFree());
        e.setTotalQuantity(r.totalQuantity());
        if (r.isActive() != null) {
            e.setIsActive(r.isActive());
        }
    }

    public static FlightCabinAncillaryResponse toResponse(FlightCabinAncillary e) {
        Ancillary a = e.getAncillary();
        Integer remaining = e.getTotalQuantity() == null
                ? null
                : e.getTotalQuantity() - e.getSoldQuantity();
        return new FlightCabinAncillaryResponse(
                e.getId(),
                e.getFlightInstanceCabinId(),
                a.getId(),
                a.getCode(),
                a.getName(),
                a.getType(),
                e.getPrice(),
                e.getCurrency(),
                e.getIsFree(),
                e.getTotalQuantity(),
                e.getSoldQuantity(),
                remaining,
                e.getIsActive(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}