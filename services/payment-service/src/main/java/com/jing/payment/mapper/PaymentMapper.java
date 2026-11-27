package com.jing.payment.mapper;

import com.jing.payment.model.Payment;
import com.jing.payment.payload.request.AbaCheckout;
import com.jing.payment.payload.response.PaymentResponse;

public final class PaymentMapper {
    private PaymentMapper() {}

    public static PaymentResponse toResponse(Payment p, AbaCheckout checkout) {
        return new PaymentResponse(p.getId(), p.getBookingId(), p.getBookingReference(), p.getTranId(),
                p.getAmount(), p.getCurrency(), p.getMethod(), p.getStatus(), p.getPaymentType(),
                p.getExpiresAt(), p.getPaidAt(), p.getFailureReason(), checkout);
    }
}