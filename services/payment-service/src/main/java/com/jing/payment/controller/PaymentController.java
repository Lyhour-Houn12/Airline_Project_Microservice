package com.jing.payment.controller;

import com.jing.common.security.CurrentUser;
import com.jing.common.security.RequireRole;
import com.jing.payment.payload.request.CreateRefundRequest;
import com.jing.payment.payload.response.PaymentRefundResponse;
import com.jing.payment.service.RefundService;
import com.jing.payment.payload.request.CreatePaymentRequest;
import com.jing.payment.payload.response.PaymentResponse;
import com.jing.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.jing.common.enumeration.UserRole.ROLE_SYSTEM_ADMIN;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final RefundService refundService;

    @PostMapping
    public ResponseEntity<PaymentResponse> create(
            CurrentUser user,
            @Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.createPayment(request, user, idempotencyKey));
    }

    @GetMapping("/{id}")
    public PaymentResponse get(CurrentUser user, @PathVariable Long id) {
        return paymentService.getPaymentById(id, user);
    }

    @GetMapping("/booking/{bookingId}")
    public List<PaymentResponse> byBooking(CurrentUser user, @PathVariable Long bookingId) {
        return paymentService.getPaymentsByBooking(bookingId, user);
    }

    @PostMapping("/{id}/sync")
    public PaymentResponse sync(CurrentUser user, @PathVariable Long id) {
        return paymentService.syncPaymentStatus(id, user);
    }

    @PostMapping("/{id}/cancel")
    public PaymentResponse cancel(CurrentUser user, @PathVariable Long id) {
        return paymentService.cancelPayment(id, user);
    }

    /** Full (no amount) or partial refund. Customers get refunds by cancelling their booking. */
    @PostMapping("/{id}/refunds")
    @RequireRole(ROLE_SYSTEM_ADMIN)
    public ResponseEntity<PaymentRefundResponse> refund(CurrentUser user, @PathVariable Long id,
                                                        @Valid @RequestBody CreateRefundRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(refundService.refund(id, request.amount(), request.reason(), "ADMIN:" + user.id()));
    }

    @GetMapping("/{id}/refunds")
    public List<PaymentRefundResponse> refunds(CurrentUser user, @PathVariable Long id) {
        paymentService.getPaymentById(id, user);          // 404 unless it's yours (or you're admin)
        return refundService.list(id);
    }
}
