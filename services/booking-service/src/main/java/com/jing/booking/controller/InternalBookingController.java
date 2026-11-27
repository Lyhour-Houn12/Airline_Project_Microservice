package com.jing.booking.controller;

import com.jing.booking.service.BookingPaymentService;
import com.jing.booking.service.FlightCancellationService;
import com.jing.common.dto.booking.FlightCancelledRequest;
import com.jing.common.dto.booking.FlightCancelledResponse;
import com.jing.common.dto.booking.BookingPaymentInfo;
import com.jing.common.dto.booking.PaymentResultRequest;
import com.jing.common.dto.booking.PaymentResultResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Used by payment-service only. Everything under {@code /internal/**} is service-to-service:
 * the API gateway must never route it, otherwise anyone could mark a booking as paid.
 */
@RestController
@RequestMapping("/internal/v1")
@RequiredArgsConstructor
public class InternalBookingController {

    private final BookingPaymentService bookingPaymentService;
    private final FlightCancellationService flightCancellationService;

    @GetMapping("/bookings/{id}/payment-info")
    public BookingPaymentInfo paymentInfo(@PathVariable Long id) {
        return bookingPaymentService.paymentInfo(id);
    }

    @PostMapping("/bookings/{id}/payment-result")
    public PaymentResultResponse paymentResult(@PathVariable Long id,
                                               @Valid @RequestBody PaymentResultRequest request) {
        return bookingPaymentService.applyPaymentResult(id, request);
    }

    /** flight-ops: the airline cancelled this flight instance. */
    @PostMapping("/flight-instances/{flightInstanceId}/cancelled")
    public FlightCancelledResponse flightCancelled(@PathVariable Long flightInstanceId,
                                                   @Valid @RequestBody FlightCancelledRequest request) {
        return flightCancellationService.cancelBookingsOn(flightInstanceId, request);
    }
}
