package com.jing.booking.service;

import com.jing.booking.client.SeatInventoryClient;
import com.jing.booking.model.Booking;
import com.jing.booking.model.BookingSegment;
import com.jing.booking.outbox.Outbox;
import com.jing.booking.service.messaging.SeatConfirmHandler;
import com.jing.booking.service.messaging.SeatReleaseHandler;
import com.jing.common.dto.seat.SeatHoldRequest;
import com.jing.common.dto.seat.SeatInstanceResponse;
import com.jing.common.dto.seat.CabinReservationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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

    /**
     * Sets this booking's places in the segment's cabin to what its passengers need. Synchronous:
     * a full cabin must stop the change right away (409 "only N seat(s) left").
     */
    public void reservePlaces(Booking booking, BookingSegment segment) {
        setPlaces(booking, segment, segment.placesNeeded());
    }

    public void reservePlacesForAll(Booking booking) {
        booking.getSegments().forEach(s -> reservePlaces(booking, s));
    }

    /**
     * Gives places back (passenger or flight removed). Best effort: if seat-service is down the hold
     * lapses with the booking anyway, so this must not block the customer's change.
     */
    public void releasePlaces(Booking booking, BookingSegment segment, int remaining) {
        try {
            setPlaces(booking, segment, remaining);
        } catch (RuntimeException e) {
            log.warn("Could not give back places of booking {} on segment {}: {}",
                    booking.getBookingReference(), segment.getId(), e.getMessage());
        }
    }

    private void setPlaces(Booking booking, BookingSegment segment, int places) {
        client.reserveCabin(new CabinReservationRequest(segment.getFlightInstanceId(), segment.getCabinClassId(),
                booking.getBookingReference(), places, booking.getExpiresAt()));
    }

    /** Book every place and seat the booking holds (after it's paid). */
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
