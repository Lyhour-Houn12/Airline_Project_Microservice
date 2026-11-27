package com.jing.booking.service;

import com.jing.booking.client.PaymentClient;
import com.jing.booking.model.Booking;
import com.jing.common.dto.payment.BookingRefundRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Asks payment-service to refund a cancelled, paid booking, once the cancellation is committed.
 * Payment-service records the refund even when it can't reach PayWay yet, and retries it;
 * when the money is back it tells us, and the booking becomes REFUNDED.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefundRequester {

    private final PaymentClient paymentClient;

    public void requestAfterCommit(Booking booking, String reason) {
        Long bookingId = booking.getId();
        String reference = booking.getBookingReference();
        Runnable call = () -> {
            try {
                paymentClient.refundBooking(new BookingRefundRequest(bookingId, reason));
                log.info("Refund requested for cancelled booking {}", reference);
            } catch (RuntimeException e) {
                log.error("REFUND NOT REQUESTED for cancelled booking {}: {} (ask payment-service again: "
                        + "POST /internal/v1/payments/refunds)", reference, e.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    call.run();
                }
            });
        } else {
            call.run();
        }
    }
}
