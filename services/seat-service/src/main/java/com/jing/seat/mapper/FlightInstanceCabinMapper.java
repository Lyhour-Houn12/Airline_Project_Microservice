package com.jing.seat.mapper;

import com.jing.common.dto.seat.FlightInstanceCabinResponse;
import com.jing.seat.model.CabinClass;
import com.jing.seat.model.FlightInstanceCabin;

public class FlightInstanceCabinMapper {

    private FlightInstanceCabinMapper() {}

    public static FlightInstanceCabin toEntity(Long flightInstanceId, CabinClass cabin,
                                               int totalSeats, Boolean isBookable) {
        return FlightInstanceCabin.builder()
                .flightInstanceId(flightInstanceId)
                .cabinClass(cabin)
                .totalSeats(totalSeats)
                .isBookable(isBookable == null || isBookable)
                .build();
    }

    public static FlightInstanceCabinResponse toResponse(FlightInstanceCabin c) {
        CabinClass cabin = c.getCabinClass();
        return new FlightInstanceCabinResponse(
                c.getId(), c.getFlightInstanceId(),
                cabin.getId(), cabin.getName(), cabin.getCode(), cabin.getCabinClassType(),
                cabin.getAircraftId(),
                c.getTotalSeats(), c.getBookedSeats(), c.getHeldSeats(), c.getAvailableSeats(),
                c.getIsBookable(), c.getCreatedAt(), c.getUpdatedAt());
    }
}