package com.jing.common.dto.booking;

import com.jing.common.enumeration.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * What payment-service needs to charge a booking (internal: payment-service -> booking-service).
 * The amount always comes from here, never from the client.
 */
public record BookingPaymentInfo(
        Long bookingId,
        String bookingReference,
        Long userId,
        BookingStatus status,
        BigDecimal totalAmount,
        String currency,
        LocalDateTime expiresAt
) {
}
