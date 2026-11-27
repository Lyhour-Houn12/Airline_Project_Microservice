package com.jing.booking.payload.request;

import jakarta.validation.constraints.Size;

public record CancelBookingRequest(@Size(max = 255) String reason) {}