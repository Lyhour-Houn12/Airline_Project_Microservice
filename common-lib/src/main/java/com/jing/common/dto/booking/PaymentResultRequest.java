package com.jing.common.dto.booking;

import com.jing.common.enumeration.PaymentStatus;
import jakarta.validation.constraints.NotNull;

/** Payment-service tells booking-service how a payment ended (internal). */
public record PaymentResultRequest(
        @NotNull Long paymentId,
        @NotNull PaymentStatus status
) {
}
