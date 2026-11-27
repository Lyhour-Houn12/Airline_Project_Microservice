package com.jing.payment.payload.response;

import com.jing.common.enumeration.PaymentMethod;
import com.jing.common.enumeration.PaymentStatus;
import com.jing.payment.payload.request.AbaCheckout;

import java.math.BigDecimal;
import java.time.Instant;
public record PaymentResponse(
        Long id, Long bookingId, String bookingReference, String tranId,
        BigDecimal amount, String currency, PaymentMethod method, PaymentStatus status,
        String paymentType, Instant expiresAt, Instant paidAt, String failureReason,
        AbaCheckout checkout            // only set on create
) {}
