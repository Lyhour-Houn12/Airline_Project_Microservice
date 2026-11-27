package com.jing.booking.payload.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SeatAssignRequest(@NotNull @Positive Long seatInstanceId) {
}
