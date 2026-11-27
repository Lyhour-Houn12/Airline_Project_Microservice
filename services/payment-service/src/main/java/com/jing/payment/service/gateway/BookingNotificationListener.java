package com.jing.payment.service.gateway;

import com.jing.payment.payload.request.PaymentStatusChangedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class BookingNotificationListener {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(PaymentStatusChangedEvent e) {
        // TODO: call booking-service via Feign once the connection exists (confirm booking / release seats)
        log.info("Payment {} for booking {} is now {}", e.paymentId(), e.bookingReference(), e.status());
    }
}
