package com.jing.flight.service.messaging;

import com.jing.common.dto.booking.FlightCancelledRequest;
import com.jing.common.dto.booking.FlightCancelledResponse;
import com.jing.flight.client.BookingClient;
import com.jing.flight.outbox.OutboxHandler;
import com.jing.flight.outbox.OutboxMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Tells booking-service that a flight instance was cancelled, so it cancels and refunds the bookings
 * on it. Through the outbox: if booking-service is down, it's retried until it gets through.
 * booking-service handles repeats (already-cancelled bookings are skipped).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlightCancelledHandler implements OutboxHandler {

    public static final String TYPE = "FLIGHT_CANCELLED";

    public record Payload(Long flightInstanceId, String flightNumber, String reason) {}

    private final BookingClient bookingClient;
    private final ObjectMapper mapper;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public void deliver(OutboxMessage message) {
        Payload p = mapper.readValue(message.getPayload(), Payload.class);
        FlightCancelledResponse result = bookingClient.flightCancelled(p.flightInstanceId(),
                new FlightCancelledRequest(p.flightNumber(), p.reason()));   // throws -> retried
        log.info("Flight {} (instance {}) cancelled: {} booking(s) cancelled", p.flightNumber(),
                p.flightInstanceId(), result.bookingsCancelled());
    }
}
