package com.jing.payment.service.gateway;

import com.jing.common.dto.booking.PaymentResultRequest;
import com.jing.common.dto.booking.PaymentResultResponse;
import com.jing.common.enumeration.PaymentStatus;
import com.jing.common.exception.BadRequestException;
import com.jing.payment.client.BookingClient;
import com.jing.payment.outbox.OutboxHandler;
import com.jing.payment.outbox.OutboxMessage;
import com.jing.payment.payload.request.PaymentStatusChangedEvent;
import com.jing.payment.service.RefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Delivers a payment result to booking-service. Any failure (booking-service down, timeout, 5xx)
 * throws, and the outbox retries. booking-service handles repeats.
 *
 * <p>If booking-service answers that the money can't be used (the booking expired, was cancelled, or was
 * already paid by another payment), this payment is refunded automatically.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingPaymentResultHandler implements OutboxHandler {

    public static final String TYPE = "PAYMENT_RESULT";

    private final BookingClient bookingClient;
    private final RefundService refundService;
    private final ObjectMapper mapper;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public void deliver(OutboxMessage message) {
        PaymentStatusChangedEvent e = mapper.readValue(message.getPayload(), PaymentStatusChangedEvent.class);
        PaymentResultResponse result = bookingClient.paymentResult(e.bookingId(),
                new PaymentResultRequest(e.paymentId(), e.status()));   // throws -> retried
        log.info("Booking {} is {}: {}", e.bookingReference(), result.status(), result.message());

        if (e.status() == PaymentStatus.SUCCESS && result.refundRequired()) {
            try {
                var refund = refundService.refund(e.paymentId(), null, "Automatic: " + result.message(), "SYSTEM");
                log.warn("Payment {} could not confirm booking {}: refund {} is {}",
                        e.paymentId(), e.bookingReference(), refund.id(), refund.status());
            } catch (BadRequestException already) {
                // a repeat of this message: the refund was already recorded, nothing left to refund
                log.info("Payment {}: {}", e.paymentId(), already.getMessage());
            }
        }
    }
}
