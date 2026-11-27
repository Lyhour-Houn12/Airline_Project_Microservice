package com.jing.common.dto.seat;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record FlightInstanceCabinRequest(
        @NotNull Long cabinClassId,
        @Min(1) Integer totalSeats, // optional: defaults to the usable seat count
        @JsonProperty("isBookable")Boolean isBookable // optional: defaults to true
) {
}
