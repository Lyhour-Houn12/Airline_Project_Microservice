package com.jing.booking.service.messaging;

import com.jing.booking.client.SeatInventoryClient;
import com.jing.booking.outbox.OutboxHandler;
import com.jing.booking.outbox.OutboxMessage;
import com.jing.common.dto.seat.SeatReleaseRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/** Gives seats back to seat-service. Releasing twice is harmless there (only the holder's seats are touched). */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeatReleaseHandler implements OutboxHandler {

    public static final String TYPE = "SEAT_RELEASE";

    /** @param seatInstanceIds null = every seat of this booking */
    public record Payload(String bookingReference, List<Long> seatInstanceIds) {}

    private final SeatInventoryClient client;
    private final ObjectMapper mapper;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public void deliver(OutboxMessage message) {
        Payload p = mapper.readValue(message.getPayload(), Payload.class);
        var result = client.release(new SeatReleaseRequest(p.bookingReference(), p.seatInstanceIds()));
        log.info("Released {} seat(s) of booking {}", result.get("released"), p.bookingReference());
    }
}
