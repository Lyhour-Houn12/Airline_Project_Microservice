package com.jing.booking.validatation;

import com.jing.booking.model.Booking;
import com.jing.booking.model.BookingSegment;
import com.jing.common.enumeration.TripType;
import com.jing.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingSegmentValidatorTest {

    private static final long PNH = 1, BKK = 2, SIN = 3;
    private final BookingSegmentValidator validator = new BookingSegmentValidator();

    @Test
    void roundTripCanAddTheReturnJourney() {
        Booking booking = booking(TripType.ROUND_TRIP);
        add(booking, PNH, BKK, 1, 5);

        // before the fix, journey 2 was rejected for every booking
        int journey = validator.validate(booking, candidate(10, BKK, PNH, 8), 2);

        assertThat(journey).isEqualTo(2);
    }

    @Test
    void oneWayCannotHaveASecondJourney() {
        Booking booking = booking(TripType.ONE_WAY);
        add(booking, PNH, BKK, 1, 5);

        assertThatThrownBy(() -> validator.validate(booking, candidate(10, BKK, SIN, 8), 2))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void completeRoundTripEndsAtTheOrigin() {
        Booking booking = booking(TripType.ROUND_TRIP);
        add(booking, PNH, BKK, 1, 5);
        add(booking, BKK, PNH, 2, 8);

        // before the fix this compared arrival with arrival and always failed
        assertThatCode(() -> validator.validateComplete(booking)).doesNotThrowAnyException();
    }

    @Test
    void roundTripEndingElsewhereIsIncomplete() {
        Booking booking = booking(TripType.ROUND_TRIP);
        add(booking, PNH, BKK, 1, 5);
        add(booking, BKK, SIN, 2, 8);

        assertThatThrownBy(() -> validator.validateComplete(booking))
                .isInstanceOf(BadRequestException.class);
    }

    private static Booking booking(TripType type) {
        return Booking.builder().tripType(type).currency("USD").build();
    }

    private static void add(Booking booking, long from, long to, int journey, int daysFromNow) {
        LocalDateTime departure = LocalDateTime.now().plusDays(daysFromNow);
        booking.addSegment(BookingSegment.builder()
                .id((long) booking.getSegments().size() + 1)
                .flightInstanceId(100L + booking.getSegments().size())
                .departureAirportId(from)
                .arrivalAirportId(to)
                .departureTime(departure)
                .arrivalTime(departure.plusHours(2))
                .journeyNumber(journey)
                .segmentOrder(booking.getSegments().size() + 1)
                .build());
    }

    private static BookingSegment candidate(long flightInstanceId, long from, long to, int daysFromNow) {
        LocalDateTime departure = LocalDateTime.now().plusDays(daysFromNow);
        return BookingSegment.builder()
                .flightInstanceId(flightInstanceId)
                .departureAirportId(from)
                .arrivalAirportId(to)
                .departureTime(departure)
                .arrivalTime(departure.plusHours(2))
                .build();
    }
}
