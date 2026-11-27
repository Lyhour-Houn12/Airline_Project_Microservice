package com.jing.booking.client;

import com.jing.common.dto.payment.BookingRefundRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** payment-service's internal API (not reachable through the gateway). */
@FeignClient(name = "payment-service", contextId = "paymentClient")
public interface PaymentClient {

    @PostMapping("/internal/v1/payments/refunds")
    void refundBooking(@RequestBody BookingRefundRequest request);
}
