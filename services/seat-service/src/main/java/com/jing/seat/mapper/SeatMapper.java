package com.jing.seat.mapper;

import com.jing.common.enumeration.SeatType;
import com.jing.common.dto.seat.SeatRequest;
import com.jing.common.dto.seat.SeatUpdateRequest;
import com.jing.common.dto.seat.SeatResponse;
import com.jing.seat.model.CabinClass;
import com.jing.seat.model.Seat;
import com.jing.seat.model.SeatMap;

public class SeatMapper {

    private SeatMapper() {}

    public static Seat toEntity(SeatRequest r, SeatMap map, char letter, String seatNumber){
        CabinClass cabin = map.getCabinClass();
        return Seat.builder()
                .aircraftId(cabin.getAircraftId())
                .seatMap(map)
                .seatNumber(seatNumber)
                .seatRow(r.seatRow())
                .columnLetter(letter)
                .seatType(r.seatType())
                .isEmergencyExit(bool(r.isEmergencyExit()))
                .hasExtraRoom(bool(r.hasExtraRoom()))
                .hasBassinet(bool(r.hasBassinet()))
                .isNearLavatory(bool(r.isNearLavatory()))
                .isNearGalley(bool(r.isNearGalley()))
                .hasPowerOutlet(bool(r.hasPowerOutlet()))
                .hasTvScreen(bool(r.hasTvScreen()))
                .isWheelchairAccessible(bool(r.isWheelchairAccessible()))
                .hasExtraWidth(bool(r.hasExtraWidth()))
                .seatPitch(r.seatPitch() != null ? r.seatPitch() : cabin.getTypicalSeatPitch())
                .seatWidth(r.seatWidth() != null ? r.seatWidth() : cabin.getTypicalSeatWidth())
                .reclineAngle(r.reclineAngle())
                .build();
    }

    public static Seat generated(SeatMap map, int row, char letter, String seatNumber, SeatType seatType){
        CabinClass cabin = map.getCabinClass();
        return Seat.builder()
                .aircraftId(cabin.getAircraftId())
                .seatMap(map)
                .seatNumber(seatNumber)
                .seatRow(row)
                .columnLetter(letter)
                .seatType(seatType)
                .seatPitch(cabin.getTypicalSeatPitch())
                .seatWidth(cabin.getTypicalSeatWidth())
                .build();
    }

    public static void updateEntity(Seat s, SeatUpdateRequest r){
        if (r.seatType() != null)  s.setSeatType(r.seatType());
        if (r.isEmergencyExit() != null) s.setIsEmergencyExit(r.isEmergencyExit());
        if (r.hasExtraRoom() != null) s.setHasExtraRoom(r.hasExtraRoom());
        if (r.hasBassinet() != null) s.setHasBassinet(r.hasBassinet());
        if (r.isNearLavatory() != null) s.setIsNearLavatory(r.isNearLavatory());
        if (r.isNearGalley() != null) s.setIsNearGalley(r.isNearGalley());
        if (r.hasPowerOutlet() != null) s.setHasPowerOutlet(r.hasPowerOutlet());
        if (r.hasTvScreen() != null) s.setHasTvScreen(r.hasTvScreen());
        if (r.isWheelchairAccessible() != null) s.setIsWheelchairAccessible(r.isWheelchairAccessible());
        if (r.hasExtraWidth() != null) s.setHasExtraWidth(r.hasExtraWidth());
        if (r.seatPitch() != null) s.setSeatPitch(r.seatPitch());
        if (r.seatWidth() != null) s.setSeatWidth(r.seatWidth());
        if (r.reclineAngle() != null) s.setReclineAngle(r.reclineAngle());
    }

    public static SeatResponse toResponse(Seat s) {
        return new SeatResponse(
                s.getId(), s.getAircraftId(),
                s.getSeatMap().getId(), s.getSeatMap().getCabinClass().getId(),
                s.getSeatNumber(), s.getSeatRow(), s.getColumnLetter(), s.getSeatType(),
                s.getIsAvailable(), s.getIsBlocked(), s.getIsActive(),
                s.getIsEmergencyExit(), s.getHasExtraRoom(), s.getHasBassinet(),
                s.getIsNearLavatory(), s.getIsNearGalley(), s.getHasPowerOutlet(),
                s.getHasTvScreen(), s.getIsWheelchairAccessible(), s.getHasExtraWidth(),
                s.getSeatPitch(), s.getSeatWidth(), s.getReclineAngle(),
                s.getCreatedAt(), s.getUpdatedAt());
    }





    private static boolean bool(Boolean b){
        return Boolean.TRUE.equals(b);
    }

}
