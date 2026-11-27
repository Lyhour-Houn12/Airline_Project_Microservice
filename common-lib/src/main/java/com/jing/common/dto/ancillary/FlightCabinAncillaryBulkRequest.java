package com.jing.common.dto.ancillary;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record FlightCabinAncillaryBulkRequest(

        @NotNull(message = "flightInstanceCabinId is required")
        @Positive(message = "flightInstanceCabinId must be positive")
        Long flightInstanceCabinId,

        @NotEmpty(message = "ancillaries must not be empty")
        @Size(max = 50, message = "ancillaries must not exceed 50 items")
        List<@Valid @NotNull(message = "ancillary item must not be null") FlightCabinAncillaryItemRequest> ancillaries
) {}