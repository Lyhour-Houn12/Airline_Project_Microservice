package com.jing.booking.service.messaging;

import com.jing.booking.client.PaymentClient;
import com.jing.booking.outbox.OutboxHandler;
import com.jing.booking.outbox.OutboxMessage;
import com.jing.common.dto.payment.BookingRefundRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Asks payment-service to refund a cancelled, paid booking. Repeats are harmless: payment-service
 * only refunds what hasn't been refunded (or requested) yet.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefundRequestHandler implements OutboxHandler {

    public static final String TYPE = "REFUND_REQUEST";

    private final PaymentClient paymentClient;
    private final ObjectMapper mapper;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public void deliver(OutboxMessage message) {
        BookingRefundRequest request = mapper.readValue(message.getPayload(), BookingRefundRequest.class);
        paymentClient.refundBooking(request);
        log.info("Refund requested for cancelled booking {}", request.bookingId());
    }
}
