package com.jing.common.dto.seat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jing.common.enumeration.SeatType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SeatRequest(
        @NotNull Long seatMapId,
        @NotNull @Min(1)  @Max(100) Integer seatRow,
        @NotNull Character columnLetter,
        @NotNull SeatType seatType,
        @JsonProperty("isEmergencyExit") Boolean isEmergencyExit,
        @JsonProperty("hasExtraRoom") Boolean hasExtraRoom,
        @JsonProperty("hasBassinet") Boolean hasBassinet,
        @JsonProperty("isNearLavatory") Boolean isNearLavatory,
        @JsonProperty("isNearGalley") Boolean isNearGalley,
        @JsonProperty("hasPowerOutlet") Boolean hasPowerOutlet,
        @JsonProperty("hasTvScreen") Boolean hasTvScreen,
        @JsonProperty("isWheelchairAccessible") Boolean isWheelchairAccessible,
        @JsonProperty("hasExtraWidth") Boolean hasExtraWidth,
        @Min(20) @Max(60) Integer seatPitch,
        @Min(10) @Max(40) Integer seatWidth,
        @Min(0) @Max(60) Integer reclineAngle
) {
}
