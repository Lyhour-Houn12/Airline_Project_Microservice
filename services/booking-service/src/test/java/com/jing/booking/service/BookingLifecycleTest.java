package com.jing.booking.service;

import com.jing.booking.model.Booking;
import com.jing.booking.model.BookingSegment;
import com.jing.booking.model.Passenger;
import com.jing.booking.model.PassengerSegment;
import com.jing.common.dto.seat.SeatConfirmResponse;
import com.jing.common.enumeration.BookingStatus;
import com.jing.common.enumeration.PassengerSegmentStatus;
import com.jing.common.enumeration.PassengerType;
import com.jing.common.enumeration.TripType;
import com.jing.common.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
    void paymentConfirmsBookingSeatsAndTickets() {
        when(seats.confirm(any(), anyList())).thenReturn(new SeatConfirmResponse(List.of(501L), List.of()));

        lifecycle.confirm(booking, 77L, "PAYMENT:77");

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getPaymentId()).isEqualTo(77L);
        assertThat(seated.getStatus()).isEqualTo(PassengerSegmentStatus.SEATED);
        assertThat(unseated.getStatus()).isEqualTo(PassengerSegmentStatus.NOT_SEATED);
        verify(seats).confirm(booking, List.of(501L));
        verify(tickets).issueTickets(booking);
    }

    @Test
    void aLostSeatIsClearedButTheBookingIsStillConfirmed() {
        seated.setSeatSurcharge(new BigDecimal("15.00"));
        when(seats.confirm(any(), anyList())).thenReturn(new SeatConfirmResponse(List.of(), List.of(501L)));

        lifecycle.confirm(booking, 77L, "PAYMENT:77");

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(seated.getSeatInstanceId()).isNull();
        assertThat(seated.getSeatSurcharge()).isEqualByComparingTo("0");
        assertThat(seated.getStatus()).isEqualTo(PassengerSegmentStatus.NOT_SEATED);
    }

    @Test
    void seatServiceDownDoesNotBlockAPaidBooking() {
        when(seats.confirm(any(), anyList())).thenThrow(new RuntimeException("seat-service unavailable"));

        lifecycle.confirm(booking, 77L, "PAYMENT:77");

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(seated.getStatus()).isEqualTo(PassengerSegmentStatus.SEAT_HELD);   // retried later
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
