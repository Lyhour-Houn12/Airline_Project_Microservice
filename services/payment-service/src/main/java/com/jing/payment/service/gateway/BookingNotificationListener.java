package com.jing.payment.service.gateway;

import com.jing.common.dto.booking.PaymentResultRequest;
import com.jing.common.dto.booking.PaymentResultResponse;
import com.jing.common.enumeration.PaymentStatus;
import com.jing.payment.client.BookingClient;
import com.jing.payment.payload.request.PaymentStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Tells booking-service when a payment succeeded, after the payment itself is committed.
 * Only SUCCESS matters to the booking: a failed or cancelled payment leaves it PENDING so
 * the customer can try again until it expires.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingNotificationListener {

    private final BookingClient bookingClient;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(PaymentStatusChangedEvent e) {
        log.info("Payment {} for booking {} is now {}", e.paymentId(), e.bookingReference(), e.status());
        if (e.status() != PaymentStatus.SUCCESS) {
            return;
        }
        try {
            PaymentResultResponse result = bookingClient.paymentResult(e.bookingId(),
                    new PaymentResultRequest(e.paymentId(), e.status()));
            if (result.refundRequired()) {
                // TODO: refund through PayWay; for now this needs a person to handle it
                log.error("REFUND REQUIRED: payment {} for booking {}: {}",
                        e.paymentId(), e.bookingReference(), result.message());
            } else {
                log.info("Booking {} is {}: {}", e.bookingReference(), result.status(), result.message());
            }
        } catch (RuntimeException ex) {
            // The payment is safe in our database. Calling POST /api/v1/payments/{id}/sync (or the next
            // PayWay callback) sends the result again; booking-service handles repeats.
            log.error("Could not tell booking-service that payment {} for booking {} succeeded: {}",
                    e.paymentId(), e.bookingReference(), ex.getMessage());
        }
    }
}
