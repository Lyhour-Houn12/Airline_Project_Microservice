package com.jing.booking.service;

import com.jing.booking.client.SeatInventoryClient;
import com.jing.booking.model.Booking;
import com.jing.booking.model.BookingSegment;
import com.jing.booking.outbox.Outbox;
import com.jing.booking.service.messaging.SeatConfirmHandler;
import com.jing.booking.service.messaging.SeatReleaseHandler;
import com.jing.common.dto.seat.SeatHoldRequest;
import com.jing.common.dto.seat.SeatInstanceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Booking-side view of seat-service. Seats are held under the booking reference (PNR).
 *
 * <ul>
 *   <li>{@link #hold} is synchronous: the customer needs to know right away if the seat is gone (409).</li>
 *   <li>Releasing and booking seats are outbox messages, saved with the booking change itself and
 *       delivered after it commits, retried until seat-service accepts them. So a rolled-back change never
 *       gives a seat away, and a seat-service outage never leaves a seat stuck.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class SeatInventory {

    private final SeatInventoryClient client;
    private final Outbox outbox;

    public SeatInstanceResponse hold(Booking booking, BookingSegment segment, Long seatInstanceId) {
        return client.hold(seatInstanceId, new SeatHoldRequest(
                segment.getFlightInstanceId(),
                segment.getCabinClassId(),
                booking.getBookingReference(),
                booking.getExpiresAt()));
    }

    /** Book every seat the booking holds (after it's paid). */
    public void confirmAfterCommit(Booking booking) {
        outbox.enqueue(SeatConfirmHandler.TYPE, booking.getId(), new SeatConfirmHandler.Payload(booking.getId()));
    }

    /** Release the given seats of this booking once the current transaction commits. */
    public void releaseAfterCommit(Booking booking, List<Long> seatInstanceIds) {
        if (seatInstanceIds.isEmpty()) return;
        outbox.enqueue(SeatReleaseHandler.TYPE, booking.getId(),
                new SeatReleaseHandler.Payload(booking.getBookingReference(), List.copyOf(seatInstanceIds)));
    }

    /** Release every seat of this booking once the current transaction commits. */
    public void releaseAllAfterCommit(Booking booking) {
        outbox.enqueue(SeatReleaseHandler.TYPE, booking.getId(),
                new SeatReleaseHandler.Payload(booking.getBookingReference(), null));
    }
}
