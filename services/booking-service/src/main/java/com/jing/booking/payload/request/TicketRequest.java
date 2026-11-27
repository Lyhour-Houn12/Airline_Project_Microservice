package com.jing.booking.payload.request;

import jakarta.validation.constraints.NotNull;

public record TicketRequest(@NotNull Long bookingId, @NotNull Long passengerSegmentId) {
}
