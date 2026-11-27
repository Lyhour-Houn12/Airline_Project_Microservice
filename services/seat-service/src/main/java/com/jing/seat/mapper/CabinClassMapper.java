package com.jing.seat.mapper;

import com.jing.common.dto.seat.CabinClassRequest;
import com.jing.common.dto.seat.CabinClassResponse;
import com.jing.seat.model.CabinClass;

import java.util.Locale;

public class CabinClassMapper {
    private CabinClassMapper() {
        // Utility class; prevent instantiation.
    }
    public static CabinClass toEntity(CabinClassRequest request) {
        if (request == null) {
            return null;
        }
        return CabinClass.builder()
                .name(request.getName())
                .cabinClassType(request.getCabinClassType())
                .code(request.getCode() != null ? request.getCode().toUpperCase(Locale.ROOT) : null)
                .description(request.getDescription())
                .aircraftId(request.getAircraftId())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .isBookable(request.getIsBookable() != null ? request.getIsBookable() : true)
                .typicalSeatPitch(request.getTypicalSeatPitch())
                .typicalSeatWidth(request.getTypicalSeatWidth())
                .seatType(request.getSeatType())
                .build();
    }

    public static CabinClassResponse toResponse(CabinClass cabinClass) {
        if (cabinClass == null) {
            return null;
        }

        return CabinClassResponse.builder()
                .id(cabinClass.getId())
                .name(cabinClass.getName())
                .cabinClassType(cabinClass.getCabinClassType())
                .code(cabinClass.getCode())
                .description(cabinClass.getDescription())
                .aircraftId(cabinClass.getAircraftId())
                .displayOrder(cabinClass.getDisplayOrder())
                .isActive(cabinClass.getIsActive())
                .isBookable(cabinClass.getIsBookable())
                .typicalSeatPitch(cabinClass.getTypicalSeatPitch())
                .typicalSeatWidth(cabinClass.getTypicalSeatWidth())
                .seatType(cabinClass.getSeatType())
                .build();
    }

    public static void updateEntity(
            CabinClassRequest request,
            CabinClass existing
    ) {
        if (request == null || existing == null) {
            return;
        }
        if (request.getName() != null) {existing.setName(request.getName());}
        if (request.getCabinClassType() != null) {existing.setCabinClassType(request.getCabinClassType());}
        if (request.getCode() != null) {existing.setCode(request.getCode().toUpperCase(Locale.ROOT));}
        if (request.getDescription() != null) {existing.setDescription(request.getDescription());}
        // aircraftId is intentionally not updated here.
        // Aircraft association is immutable through ordinary updates.
        if (request.getDisplayOrder() != null) {existing.setDisplayOrder(request.getDisplayOrder());}
        if (request.getIsActive() != null) {existing.setIsActive(request.getIsActive());}
        if (request.getIsBookable() != null) {existing.setIsBookable(request.getIsBookable());}
        if (request.getTypicalSeatPitch() != null) {existing.setTypicalSeatPitch(request.getTypicalSeatPitch());}
        if (request.getTypicalSeatWidth() != null) {existing.setTypicalSeatWidth(request.getTypicalSeatWidth());}
        if (request.getSeatType() != null) {existing.setSeatType(request.getSeatType());}
    }
}