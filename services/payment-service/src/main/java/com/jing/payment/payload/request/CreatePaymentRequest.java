package com.jing.payment.payload.request;

import com.jing.common.enumeration.PaymentMethod;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Start paying a booking. Amount, currency and booking reference are NOT accepted from the client:
 * they are read from booking-service, so nobody can pay $0.01 for a $500 booking.
 */
public record CreatePaymentRequest(
        @NotNull Long bookingId,
        @NotNull PaymentMethod method,
        @Size(max = 20) String firstName,
        @Size(max = 20) String lastName,
        @Email @Size(max = 50) String email,
        @Size(max = 20) String phone,
        @Size(max = 20) String paymentOption               // blank = customer chooses on PayWay page
) {
}
