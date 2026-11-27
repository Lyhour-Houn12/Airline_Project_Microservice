package com.jing.common.dto.booking;

import com.jing.common.enumeration.BookingStatus;

/**
 * What booking-service did with a payment result.
 *
 * @param refundRequired true when money arrived for a booking that can no longer be confirmed
 *                       (it expired or was cancelled first); payment-service must refund it
 */
public record PaymentResultResponse(
        Long bookingId,
        BookingStatus status,
        boolean refundRequired,
        String message
) {
}
