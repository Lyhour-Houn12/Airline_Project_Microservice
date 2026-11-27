package com.jing.common.dto.seat;


import com.jing.common.enumeration.SeatAvailabilityStatus;
import com.jing.common.enumeration.SeatType;

import java.math.BigDecimal;
import java.util.List;

public record FlightSeatMapResponse(Long flightInstanceId, List<CabinSeatMap> cabins) {

    public record CabinSeatMap(
            Long flightInstanceCabinId,
            String cabinClassName,
            int totalSeats,
            int availableSeats,
            List<SeatRow> rows) {}

    public record SeatRow(int rowNumber, List<SeatInstanceDto> seats) {}

    public record SeatInstanceDto(
            Long seatInstanceId,
            String seatNumber,
            String columnLetter,
            SeatType seatType,
            SeatAvailabilityStatus status,
            BigDecimal surcharge) {}
}
