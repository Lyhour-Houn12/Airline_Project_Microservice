package com.jing.payment.service.gateway;

import com.jing.common.enumeration.PaymentStatus;
import com.jing.payment.outbox.Outbox;
import com.jing.payment.payload.request.PaymentStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Payments that booking-service must hear about are written to the outbox IN the payment's own
 * transaction (BEFORE_COMMIT). So a SUCCESS can't be lost: if booking-service is down, the outbox keeps
 * delivering it until booking confirms. Delivery itself is {@link BookingPaymentResultHandler}.
 *
 * <p>Only SUCCESS and REFUNDED matter to the booking. A failed or cancelled payment leaves it PENDING,
 * so the customer can try again until it expires.
 */
@Component
@RequiredArgsConstructor
public class BookingNotificationListener {

    private final Outbox outbox;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void on(PaymentStatusChangedEvent e) {
        if (e.status() == PaymentStatus.SUCCESS || e.status() == PaymentStatus.REFUNDED) {
            outbox.enqueue(BookingPaymentResultHandler.TYPE, e.paymentId(), e);
        }
    }
}
