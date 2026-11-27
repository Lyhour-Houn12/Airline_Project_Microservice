package com.jing.common.dto.payment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Refund what was paid for a booking (internal: booking-service -> payment-service), e.g. when a
 * confirmed booking is cancelled. Payment-service finds the successful payment and refunds the rest of it.
 */
public record BookingRefundRequest(
        @NotNull Long bookingId,
        @Size(max = 255) String reason
) {
}
