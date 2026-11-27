package com.jing.booking.service;

import com.jing.booking.model.Booking;
import com.jing.booking.model.BookingSegment;
import com.jing.booking.model.Passenger;
import com.jing.booking.model.PassengerSegment;
import com.jing.common.enumeration.BookingStatus;
import com.jing.common.enumeration.PassengerSegmentStatus;
import com.jing.common.enumeration.PassengerType;
import com.jing.common.enumeration.TripType;
import com.jing.common.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BookingLifecycleTest {

    private final SeatInventory seats = mock(SeatInventory.class);
    private final TicketService tickets = mock(TicketService.class);
    private final RefundRequester refunds = mock(RefundRequester.class);
    private final BookingLifecycle lifecycle = new BookingLifecycle(new BookingStateMachine(), seats, tickets, refunds);

    private Booking booking;
    private PassengerSegment seated;    // holds seat 501
    private PassengerSegment unseated;  // no seat chosen

    @BeforeEach
    void setUp() {
        booking = Booking.builder().id(1L).bookingReference("ABC123").tripType(TripType.ONE_WAY)
                .status(BookingStatus.PENDING).currency("USD").build();
        BookingSegment segment = BookingSegment.builder().id(10L).build();
        booking.addSegment(segment);
        seated = link(segment, 501L);
        unseated = link(segment, null);
    }

    private PassengerSegment link(BookingSegment segment, Long seatId) {
        Passenger p = Passenger.builder().passengerType(PassengerType.ADULT).build();
        booking.addPassenger(p);
        PassengerSegment ps = booking.link(p, segment);
        ps.setSeatInstanceId(seatId);
        ps.setStatus(seatId != null ? PassengerSegmentStatus.SEAT_HELD : PassengerSegmentStatus.NOT_SEATED);
        return ps;
    }




    @Test
    void paymentConfirmsBookingAndQueuesTheSeatsAndTickets() {
        lifecycle.confirm(booking, 77L, "PAYMENT:77");

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getPaymentId()).isEqualTo(77L);
        assertThat(booking.getExpiresAt()).isNull();
        verify(seats).confirmAfterCommit(booking);          // booked by the outbox after commit
        verify(tickets).issueTickets(booking);
    }

    @Test
    void noSeatMessageWhenNobodyPickedASeat() {
        seated.setSeatInstanceId(null);
        seated.setStatus(PassengerSegmentStatus.NOT_SEATED);

        lifecycle.confirm(booking, 77L, "PAYMENT:77");

        verify(seats, never()).confirmAfterCommit(any());
    }

    @Test
    void expiringReleasesSeatsWithoutTouchingTickets() {
        lifecycle.close(booking, BookingStatus.EXPIRED, "SYSTEM", "Not paid in time");

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(seated.getStatus()).isEqualTo(PassengerSegmentStatus.CANCELLED);
        verify(seats).releaseAllAfterCommit(booking);
        verifyNoInteractions(tickets);
    }

    @Test
    void cancellingAPaidBookingVoidsItsTickets() {
        booking.setStatus(BookingStatus.CONFIRMED);

        lifecycle.close(booking, BookingStatus.CANCELLED, "5", "Plans changed");

        verify(tickets).voidIssuedTickets(booking);
        verify(seats).releaseAllAfterCommit(booking);
        verify(refunds).requestAfterCommit(booking, "Plans changed");
    }

    @Test
    void expiringAnUnpaidBookingRequestsNoRefund() {
        lifecycle.close(booking, BookingStatus.EXPIRED, "SYSTEM", "Not paid in time");

        verifyNoInteractions(refunds);
    }

    @Test
    void anExpiredBookingCannotBeConfirmed() {
        booking.setStatus(BookingStatus.EXPIRED);

        assertThatThrownBy(() -> lifecycle.confirm(booking, 77L, "PAYMENT:77"))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(seats, tickets);
    }
}
