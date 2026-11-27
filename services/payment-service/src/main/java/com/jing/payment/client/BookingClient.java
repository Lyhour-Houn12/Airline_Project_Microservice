package com.jing.payment.client;

import com.jing.common.dto.booking.BookingPaymentInfo;
import com.jing.common.dto.booking.PaymentResultRequest;
import com.jing.common.dto.booking.PaymentResultResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** booking-service's internal payment API (not reachable through the gateway). */
@FeignClient(name = "booking-service", contextId = "bookingClient")
public interface BookingClient {

    @GetMapping("/internal/v1/bookings/{id}/payment-info")
    BookingPaymentInfo paymentInfo(@PathVariable("id") Long bookingId);

    @PostMapping("/internal/v1/bookings/{id}/payment-result")
    PaymentResultResponse paymentResult(@PathVariable("id") Long bookingId, @RequestBody PaymentResultRequest request);
}
