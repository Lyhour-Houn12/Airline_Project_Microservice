package com.jing.common.dto.seat;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;

public record FlightInstanceCabinUpdateRequest(
        @Min(1) Integer totalSeats,
        @JsonProperty("isBookable") Boolean isBookable
) {}