package com.jing.booking.service;

import com.jing.booking.model.Booking;
import com.jing.booking.model.PassengerSegment;
import com.jing.common.dto.seat.SeatConfirmResponse;
import com.jing.common.enumeration.BookingStatus;
import com.jing.common.enumeration.PassengerSegmentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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
     * Book every seat this booking still holds. Seats whose hold was lost are cleared: the booking
     * stays confirmed and the passenger can pick a seat later (check-in). Safe to call again.
     */
    public void confirmHeldSeats(Booking booking) {
        List<PassengerSegment> held = booking.passengerSegments()
                .filter(ps -> ps.getStatus() == PassengerSegmentStatus.SEAT_HELD && ps.getSeatInstanceId() != null)
                .toList();
        if (held.isEmpty()) return;

        SeatConfirmResponse result;
        try {
            result = seatInventory.confirm(booking, held.stream().map(PassengerSegment::getSeatInstanceId).toList());
        } catch (RuntimeException e) {
            // the booking is paid: never fail the confirmation because seat-service is down.
            // The seats stay SEAT_HELD; the next payment-result call (or an admin) retries.
            log.error("Booking {} is paid but its seats could not be confirmed: {}",
                    booking.getBookingReference(), e.getMessage());
            return;
        }
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

    /** CANCELLED or EXPIRED: give every seat back and void unused tickets. */
    public void close(Booking booking, BookingStatus target, String changedBy, String reason) {
        if (target != BookingStatus.CANCELLED && target != BookingStatus.EXPIRED) {
            throw new IllegalArgumentException("close() is for CANCELLED or EXPIRED, not " + target);
        }
        BookingStatus before = booking.getStatus();
        stateMachine.assertTransition(before, target);
        booking.changeStatus(target, changedBy, reason);

        boolean hadSeats = booking.passengerSegments().map(PassengerSegment::getSeatInstanceId).anyMatch(Objects::nonNull);
        booking.passengerSegments().forEach(ps -> ps.setStatus(PassengerSegmentStatus.CANCELLED));
        if (hadSeats) {
            seatInventory.releaseAllAfterCommit(booking);
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
