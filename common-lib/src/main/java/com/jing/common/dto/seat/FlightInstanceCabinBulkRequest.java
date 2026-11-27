package com.jing.common.dto.seat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record FlightInstanceCabinBulkRequest(
        @NotNull @Positive Long flightInstanceId,
        @NotEmpty @Size(max = 10) List<@Valid FlightInstanceCabinRequest> cabins
) {}