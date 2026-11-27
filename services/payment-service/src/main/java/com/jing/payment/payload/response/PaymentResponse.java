package com.jing.payment.payload.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jing.common.enumeration.PaymentMethod;
import com.jing.common.enumeration.PaymentStatus;
import com.jing.payment.payload.request.AbaCheckout;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A payment. Right after creation exactly one of {@code checkout} (ABA_PAYWAY: post these fields to
 * PayWay's page) or {@code qr} (ABA_PAYWAY_QR: show the QR / open the deeplink) is filled in.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentResponse(
        Long id, Long bookingId, String bookingReference, String tranId,
        BigDecimal amount, BigDecimal refundedAmount, String currency, PaymentMethod method, PaymentStatus status,
        String paymentType, Instant expiresAt, Instant paidAt, String failureReason,
        AbaCheckout checkout,
        AbaQr qr
) {}
