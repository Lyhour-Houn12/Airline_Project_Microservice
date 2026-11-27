package com.jing.booking.service;

import com.jing.booking.client.SeatInventoryClient;
import com.jing.booking.model.Booking;
import com.jing.booking.model.BookingSegment;
import com.jing.common.dto.seat.SeatConfirmRequest;
import com.jing.common.dto.seat.SeatConfirmResponse;
import com.jing.common.dto.seat.SeatHoldRequest;
import com.jing.common.dto.seat.SeatInstanceResponse;
import com.jing.common.dto.seat.SeatReleaseRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * Booking-side view of seat-service. Seats are held under the booking reference (PNR).
 *
 * <ul>
 *   <li>{@link #hold} is synchronous: the customer needs to know right away if the seat is gone (409).</li>
 *   <li>Releases run only after the booking change has committed, so a rolled-back change never
 *       gives away a seat the booking still points to. They are best effort: if seat-service is down
 *       a HELD seat frees itself when its hold lapses, a BOOKED seat is logged for manual release.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeatInventory {

    private final SeatInventoryClient client;

    public SeatInstanceResponse hold(Booking booking, BookingSegment segment, Long seatInstanceId) {
        return client.hold(seatInstanceId, new SeatHoldRequest(
                segment.getFlightInstanceId(),
                segment.getCabinClassId(),
                booking.getBookingReference(),
                booking.getExpiresAt()));
    }

    public SeatConfirmResponse confirm(Booking booking, List<Long> seatInstanceIds) {
        return client.confirm(new SeatConfirmRequest(booking.getBookingReference(), seatInstanceIds));
    }

    /** Release the given seats of this booking once the current transaction commits. */
    public void releaseAfterCommit(Booking booking, List<Long> seatInstanceIds) {
        if (seatInstanceIds.isEmpty()) return;
        afterCommit(booking.getBookingReference(), List.copyOf(seatInstanceIds));
    }

    /** Release every seat of this booking once the current transaction commits. */
    public void releaseAllAfterCommit(Booking booking) {
        afterCommit(booking.getBookingReference(), null);
    }

    private void afterCommit(String reference, List<Long> ids) {
        Runnable release = () -> {
            try {
                client.release(new SeatReleaseRequest(reference, ids));
            } catch (RuntimeException e) {
                log.error("Could not release seats {} of booking {}: {}",
                        ids == null ? "(all)" : ids, reference, e.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    release.run();
                }
            });
        } else {
            release.run();
        }
    }
}
