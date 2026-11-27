package com.jing.booking.service.messaging;

import com.jing.booking.client.SeatInventoryClient;
import com.jing.booking.model.Booking;
import com.jing.booking.model.PassengerSegment;
import com.jing.booking.outbox.OutboxHandler;
import com.jing.booking.outbox.OutboxMessage;
import com.jing.booking.repository.BookingRepository;
import com.jing.common.dto.seat.CabinConfirmRequest;
import com.jing.common.dto.seat.SeatConfirmRequest;
import com.jing.common.dto.seat.SeatConfirmResponse;
import com.jing.common.enumeration.BookingStatus;
import com.jing.common.enumeration.PassengerSegmentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * After payment: turns the booking's HELD seats into BOOKED seats in seat-service, then records the
 * outcome on the passengers (runs inside the outbox's transaction). A seat whose hold was lost (it
 * lapsed and someone else took it) is cleared: the booking stays confirmed and the passenger picks a seat
 * later. Safe to repeat: seat-service answers "confirmed" again for seats this booking already booked.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeatConfirmHandler implements OutboxHandler {

    public static final String TYPE = "SEAT_CONFIRM";

    public record Payload(Long bookingId) {}

    private final BookingRepository bookingRepository;
    private final SeatInventoryClient client;
    private final ObjectMapper mapper;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public void deliver(OutboxMessage message) {
        Long bookingId = mapper.readValue(message.getPayload(), Payload.class).bookingId();
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null || booking.getStatus() != BookingStatus.CONFIRMED) {
            return;   // deleted or cancelled meanwhile: cancelling already released its seats
        }
        // 1. the places in each cabin (every passenger who needs one)
        client.confirmCabins(new CabinConfirmRequest(booking.getBookingReference(), booking.getSegments().stream()
                .map(s -> new CabinConfirmRequest.Item(s.getFlightInstanceId(), s.getCabinClassId(), s.placesNeeded()))
                .toList()));   // throws -> retried

        // 2. the seats passengers picked
        List<PassengerSegment> held = booking.passengerSegments()
                .filter(ps -> ps.getStatus() == PassengerSegmentStatus.SEAT_HELD && ps.getSeatInstanceId() != null)
                .toList();
        if (held.isEmpty()) return;

        SeatConfirmResponse result = client.confirm(new SeatConfirmRequest(booking.getBookingReference(),
                held.stream().map(PassengerSegment::getSeatInstanceId).toList()));   // throws -> retried
        apply(booking, held, result);
    }

    static void apply(Booking booking, List<PassengerSegment> held, SeatConfirmResponse result) {
        Set<Long> confirmed = new HashSet<>(result.confirmed());
        for (PassengerSegment ps : held) {
            if (confirmed.contains(ps.getSeatInstanceId())) {
                ps.setStatus(PassengerSegmentStatus.SEATED);
            } else {
                ps.setSeatInstanceId(null);
                ps.setSeatSurcharge(BigDecimal.ZERO);
                ps.setStatus(PassengerSegmentStatus.NOT_SEATED);
            }
        }
        if (!result.rejected().isEmpty()) {
            log.warn("Booking {} lost {} seat(s) before payment; passengers must pick new seats",
                    booking.getBookingReference(), result.rejected().size());
        }
    }
}
