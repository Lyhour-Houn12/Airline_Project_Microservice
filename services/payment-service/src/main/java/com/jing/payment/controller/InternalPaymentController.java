package com.jing.payment.controller;

import com.jing.common.dto.payment.BookingRefundRequest;
import com.jing.payment.payload.response.PaymentRefundResponse;
import com.jing.payment.service.RefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Used by booking-service only. {@code /internal/**} is service-to-service:
 * the API gateway must never route it, otherwise anyone could trigger refunds.
 */
@RestController
@RequestMapping("/internal/v1/payments")
@RequiredArgsConstructor
public class InternalPaymentController {

    private final RefundService refundService;

    /** Refund what was paid for a cancelled booking. 204 when there was nothing (left) to refund. */
    @PostMapping("/refunds")
    public ResponseEntity<PaymentRefundResponse> refundBooking(@Valid @RequestBody BookingRefundRequest request) {
        String reason = request.reason() != null ? request.reason() : "Booking cancelled";
        return refundService.refundBooking(request.bookingId(), reason, "BOOKING-SERVICE")
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
