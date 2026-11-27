package com.jing.payment.service;

import com.jing.common.security.CurrentUser;
import com.jing.payment.payload.request.CreatePaymentRequest;
import com.jing.payment.payload.response.PaymentResponse;
import tools.jackson.databind.JsonNode;

import java.util.List;

public interface PaymentService {

    /** The payer is always the caller: the user id comes from the verified headers, never the body. */
    PaymentResponse createPayment(CreatePaymentRequest request, CurrentUser user, String idempotencyKey);

    PaymentResponse getPaymentById(Long paymentId, CurrentUser user);

    List<PaymentResponse> getPaymentsByBooking(Long bookingId, CurrentUser user);

    /** Ask PayWay for the latest status of this payment and update it. */
    PaymentResponse syncPaymentStatus(Long paymentId, CurrentUser user);

    PaymentResponse cancelPayment(Long paymentId, CurrentUser user);

    /** Called by the PayWay callback endpoint after the signature is verified. */
    void processAbaCallback(JsonNode body, String rawBody);

    /** Internal, used by the expiry job: check PayWay, then mark paid or expired. */
    void reconcilePendingPayment(Long paymentId);
}
