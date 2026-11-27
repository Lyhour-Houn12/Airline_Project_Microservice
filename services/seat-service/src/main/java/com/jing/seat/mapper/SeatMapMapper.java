package com.jing.seat.mapper;

import com.jing.common.dto.seat.SeatMapRequest;
import com.jing.common.dto.seat.SeatMapResponse;
import com.jing.seat.model.CabinClass;
import com.jing.seat.model.SeatMap;

public class SeatMapMapper {

    private SeatMapMapper() {
    }


    public static SeatMap toEntity(SeatMapRequest request, CabinClass cabinClass) {
        return SeatMap.builder()
                .name(request.getName())
                .totalRows(request.getTotalRows())
                .leftSeatsPerRow(request.getLeftSeatsPerRow())
                .rightSeatsPerRow(request.getRightSeatsPerRow())
                .airlineId(request.getAirlineId())
                .cabinClass(cabinClass)
                .build();
    }

    public static void updateEntity(SeatMap seatMap, SeatMapRequest request, CabinClass cabinClass) {
        seatMap.setName(request.getName());
        seatMap.setTotalRows(request.getTotalRows());
        seatMap.setLeftSeatsPerRow(request.getLeftSeatsPerRow());
        seatMap.setRightSeatsPerRow(request.getRightSeatsPerRow());
        seatMap.setAirlineId(request.getAirlineId());
        seatMap.setCabinClass(cabinClass);
    }

    public static SeatMapResponse toResponse(SeatMap seatMap) {
        return new SeatMapResponse(
                seatMap.getId(),
                seatMap.getName(),
                seatMap.getTotalRows(),
                seatMap.getLeftSeatsPerRow(),
                seatMap.getRightSeatsPerRow(),
                seatMap.getAirlineId(),
                seatMap.getCabinClass().getId(),
                seatMap.getCabinClass().getName(),
                seatMap.getCabinClass().getCode()
        );
    }
}