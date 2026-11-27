package com.jing.booking.service;

import com.jing.booking.model.Booking;
import com.jing.booking.outbox.Outbox;
import com.jing.booking.service.messaging.RefundRequestHandler;
import com.jing.common.dto.payment.BookingRefundRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Asks payment-service to refund a cancelled, paid booking: an outbox message saved with the
 * cancellation, delivered after it commits and retried until payment-service accepts it.
 * When the money is back payment-service tells us, and the booking becomes REFUNDED.
 */
@Component
@RequiredArgsConstructor
public class RefundRequester {

    private final Outbox outbox;

    public void requestAfterCommit(Booking booking, String reason) {
        outbox.enqueue(RefundRequestHandler.TYPE, booking.getId(), new BookingRefundRequest(booking.getId(), reason));
    }
}
