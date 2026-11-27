package com.jing.payment.payload.request;

import com.jing.common.enumeration.PaymentMethod;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record  CreatePaymentRequest(
        @NotNull Long bookingId,
        @NotBlank @Size(max = 50) String bookingReference,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @Pattern(regexp = "USD|KHR") String currency,      // default USD
        @NotNull PaymentMethod method,
        @Size(max = 20) String firstName,
        @Size(max = 20) String lastName,
        @Email @Size(max = 50) String email,
        @Size(max = 20) String phone,
        @Size(max = 20) String paymentOption               // blank = customer chooses on PayWay page
) {
}
