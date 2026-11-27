package com.jing.ancillary.mapper;

import com.jing.ancillary.model.Ancillary;
import com.jing.common.dto.ancillary.AncillaryRequest;
import com.jing.common.dto.ancillary.AncillaryResponse;

public final class AncillaryMapper {

    private AncillaryMapper() {}

    public static Ancillary toEntity(AncillaryRequest r) {
        return Ancillary.builder()
                .airlineId(r.airlineId())
                .code(r.code().trim().toUpperCase())
                .name(r.name().trim())
                .description(r.description() == null ? null : r.description().trim())
                .type(r.type())
                .basePrice(r.basePrice())
                .currency(r.currency().trim().toUpperCase())
                .isActive(r.isActive() == null ? Boolean.TRUE : r.isActive())
                .build();
    }

    // airlineId and code are immutable, so they are not touched here
    public static void updateEntity(Ancillary e, AncillaryRequest r) {
        e.setName(r.name().trim());
        e.setDescription(r.description() == null ? null : r.description().trim());
        e.setType(r.type());
        e.setBasePrice(r.basePrice());
        e.setCurrency(r.currency().trim().toUpperCase());
        if (r.isActive() != null) {
            e.setIsActive(r.isActive());
        }
    }

    public static AncillaryResponse toResponse(Ancillary e) {
        return new AncillaryResponse(
                e.getId(),
                e.getAirlineId(),
                e.getCode(),
                e.getName(),
                e.getDescription(),
                e.getType(),
                e.getBasePrice(),
                e.getCurrency(),
                e.getIsActive(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}

