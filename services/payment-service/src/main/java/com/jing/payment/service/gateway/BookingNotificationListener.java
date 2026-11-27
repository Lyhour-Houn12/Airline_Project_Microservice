package com.jing.payment.service.gateway;

import com.jing.common.dto.booking.PaymentResultRequest;
import com.jing.common.dto.booking.PaymentResultResponse;
import com.jing.common.enumeration.PaymentStatus;
import com.jing.payment.client.BookingClient;
import com.jing.payment.payload.request.PaymentStatusChangedEvent;
import com.jing.payment.service.RefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Tells booking-service about payments that matter to it, after the payment change is committed:
 * <ul>
 *   <li>SUCCESS: the booking gets confirmed. If booking-service answers that it can't be (it expired,
 *       was cancelled, or was already paid by another payment), this payment is refunded automatically.</li>
 *   <li>REFUNDED: a cancelled booking becomes REFUNDED.</li>
 * </ul>
 * A failed or cancelled payment leaves the booking PENDING, so the customer can try again until it expires.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingNotificationListener {

    private final BookingClient bookingClient;
    private final RefundService refundService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(PaymentStatusChangedEvent e) {
        log.info("Payment {} for booking {} is now {}", e.paymentId(), e.bookingReference(), e.status());
        if (e.status() != PaymentStatus.SUCCESS && e.status() != PaymentStatus.REFUNDED) {
            return;
        }
        PaymentResultResponse result;
        try {
            result = bookingClient.paymentResult(e.bookingId(), new PaymentResultRequest(e.paymentId(), e.status()));
        } catch (RuntimeException ex) {
            // The payment is safe in our database. POST /api/v1/payments/{id}/sync (or the next
            // PayWay callback) sends the result again; booking-service handles repeats.
            log.error("Could not tell booking-service that payment {} for booking {} is {}: {}",
                    e.paymentId(), e.bookingReference(), e.status(), ex.getMessage());
            return;
        }
        log.info("Booking {} is {}: {}", e.bookingReference(), result.status(), result.message());

        if (e.status() == PaymentStatus.SUCCESS && result.refundRequired()) {
            try {
                var refund = refundService.refund(e.paymentId(), null,
                        "Automatic: " + result.message(), "SYSTEM");
                log.warn("Payment {} could not confirm booking {}: refund {} is {}",
                        e.paymentId(), e.bookingReference(), refund.id(), refund.status());
            } catch (RuntimeException ex) {
                log.error("REFUND REQUIRED but could not be recorded for payment {} (booking {}): {}",
                        e.paymentId(), e.bookingReference(), ex.getMessage());
            }
        }
    }
}
