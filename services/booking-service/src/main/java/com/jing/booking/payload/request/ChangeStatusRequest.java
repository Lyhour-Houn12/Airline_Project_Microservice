package com.jing.booking.payload.request;

import com.jing.common.enumeration.BookingStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangeStatusRequest(
        @NotNull BookingStatus status,
        @Size(max = 255) String reason) {}