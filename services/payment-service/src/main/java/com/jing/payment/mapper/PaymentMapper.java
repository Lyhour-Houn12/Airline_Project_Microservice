package com.jing.payment.mapper;

import com.jing.payment.model.Payment;
import com.jing.payment.payload.request.AbaCheckout;
import com.jing.payment.payload.response.AbaQr;
import com.jing.payment.payload.response.PaymentResponse;

public final class PaymentMapper {
    private PaymentMapper() {}

    public static PaymentResponse toResponse(Payment p) {
        return toResponse(p, null, null);
    }

    public static PaymentResponse toResponse(Payment p, AbaCheckout checkout, AbaQr qr) {
        return new PaymentResponse(p.getId(), p.getBookingId(), p.getBookingReference(), p.getTranId(),
                p.getAmount(), p.getRefundedAmount(), p.getCurrency(), p.getMethod(), p.getStatus(),
                p.getPaymentType(), p.getExpiresAt(), p.getPaidAt(), p.getFailureReason(), checkout, qr);
    }
}
