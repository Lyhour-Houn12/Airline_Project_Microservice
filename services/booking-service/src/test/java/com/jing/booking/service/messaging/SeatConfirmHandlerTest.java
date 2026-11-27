package com.jing.booking.service.messaging;

import com.jing.booking.model.Booking;
import com.jing.booking.model.BookingSegment;
import com.jing.booking.model.Passenger;
import com.jing.booking.model.PassengerSegment;
import com.jing.common.dto.seat.SeatConfirmResponse;
import com.jing.common.enumeration.BookingStatus;
import com.jing.common.enumeration.PassengerSegmentStatus;
import com.jing.common.enumeration.PassengerType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SeatConfirmHandlerTest {

    @Test
    void bookedSeatsBecomeSeatedAndLostSeatsAreCleared() {
        Booking booking = Booking.builder().id(1L).bookingReference("ABC123").status(BookingStatus.CONFIRMED).build();
        BookingSegment segment = BookingSegment.builder().id(10L).build();
        booking.addSegment(segment);
        PassengerSegment kept = held(booking, segment, 501L);
        PassengerSegment lost = held(booking, segment, 502L);
        lost.setSeatSurcharge(new BigDecimal("15.00"));

        SeatConfirmHandler.apply(booking, List.of(kept, lost), new SeatConfirmResponse(List.of(501L), List.of(502L)));

        assertThat(kept.getStatus()).isEqualTo(PassengerSegmentStatus.SEATED);
        assertThat(kept.getSeatInstanceId()).isEqualTo(501L);
        assertThat(lost.getStatus()).isEqualTo(PassengerSegmentStatus.NOT_SEATED);
        assertThat(lost.getSeatInstanceId()).isNull();
        assertThat(lost.getSeatSurcharge()).isEqualByComparingTo("0");
    }

    private static PassengerSegment held(Booking booking, BookingSegment segment, Long seatId) {
        Passenger p = Passenger.builder().passengerType(PassengerType.ADULT).build();
        booking.addPassenger(p);
        PassengerSegment ps = booking.link(p, segment);
        ps.setSeatInstanceId(seatId);
        ps.setStatus(PassengerSegmentStatus.SEAT_HELD);
        return ps;
    }
}
