package com.jing.payment.controller;

import com.jing.common.security.CurrentUser;
import com.jing.payment.payload.request.CreatePaymentRequest;
import com.jing.payment.payload.response.PaymentResponse;
import com.jing.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

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
}
