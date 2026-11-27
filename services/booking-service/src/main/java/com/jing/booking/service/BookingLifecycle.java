package com.jing.booking.service;

import com.jing.booking.model.Booking;
import com.jing.common.enumeration.BookingStatus;
import com.jing.common.enumeration.PassengerSegmentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


/**
 * The status changes that have side effects in other services, in one place:
 *
 * <pre>
 *   PENDING --paid--> CONFIRMED   seats HELD -> BOOKED, tickets issued
 *   PENDING --------> EXPIRED     seats released (the expiry job)
 *   PENDING/CONFIRMED -> CANCELLED seats released, tickets voided, paid ones refunded
 *   CANCELLED -------> REFUNDED   payment-service reports the money is back
 * </pre>
 * Callers must be inside a transaction.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingLifecycle {

    private final BookingStateMachine stateMachine;
    private final SeatInventory seatInventory;
    private final TicketService ticketService;
    private final RefundRequester refundRequester;

    /** Confirm a paid booking. {@code paymentId} is null when an admin confirms it by hand. */
    public void confirm(Booking booking, Long paymentId, String changedBy) {
        stateMachine.assertTransition(booking.getStatus(), BookingStatus.CONFIRMED);
        booking.changeStatus(BookingStatus.CONFIRMED, changedBy,
                paymentId != null ? "Paid (payment " + paymentId + ")" : "Confirmed by admin");
        booking.setPaymentId(paymentId);
        booking.setExpiresAt(null);   // nothing left to expire
        confirmHeldSeats(booking);
        ticketService.issueTickets(booking);
    }

    /**
     * Book every seat this booking still holds. Sent through the outbox (see SeatConfirmHandler), so
     * a seat-service outage only delays it. Seats whose hold was lost are cleared there; the booking
     * stays confirmed. Safe to call again.
     */
    public void confirmHeldSeats(Booking booking) {
        // always: the cabin places of every passenger are booked, seats picked or not
        seatInventory.confirmAfterCommit(booking);
    }

    /** CANCELLED or EXPIRED: give every seat back and void unused tickets. */
    public void close(Booking booking, BookingStatus target, String changedBy, String reason) {
        if (target != BookingStatus.CANCELLED && target != BookingStatus.EXPIRED) {
            throw new IllegalArgumentException("close() is for CANCELLED or EXPIRED, not " + target);
        }
        BookingStatus before = booking.getStatus();
        stateMachine.assertTransition(before, target);
        booking.changeStatus(target, changedBy, reason);

        booking.passengerSegments().forEach(ps -> ps.setStatus(PassengerSegmentStatus.CANCELLED));
        if (!booking.getSegments().isEmpty()) {
            seatInventory.releaseAllAfterCommit(booking);   // seats and cabin places
        }
        if (before == BookingStatus.CONFIRMED) {
            int voided = ticketService.voidIssuedTickets(booking);
            // TODO: apply the fare rules (cancellation fee, non-refundable fares); for now it's a full refund
            refundRequester.requestAfterCommit(booking, reason);
            log.info("Booking {} cancelled after payment: {} ticket(s) voided, refund of payment {} requested",
                    booking.getBookingReference(), voided, booking.getPaymentId());
        }
    }
}
