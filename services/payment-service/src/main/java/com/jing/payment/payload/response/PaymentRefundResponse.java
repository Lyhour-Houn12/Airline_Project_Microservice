package com.jing.payment.payload.response;

import com.jing.payment.model.PaymentRefund;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentRefundResponse(
        Long id,
        Long paymentId,
        BigDecimal amount,
        String currency,
        PaymentRefund.Status status,
        String reason,
        String requestedBy,
        String providerCode,
        String providerMessage,
        int attempts,
        Instant createdAt,
        Instant updatedAt
) {
    public static PaymentRefundResponse from(PaymentRefund r) {
        return new PaymentRefundResponse(r.getId(), r.getPaymentId(), r.getAmount(), r.getCurrency(),
                r.getStatus(), r.getReason(), r.getRequestedBy(), r.getProviderCode(), r.getProviderMessage(),
                r.getAttempts(), r.getCreatedAt(), r.getUpdatedAt());
    }
}
