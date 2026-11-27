package com.jing.booking.validatation;

import com.jing.booking.model.Booking;
import com.jing.booking.model.BookingSegment;
import com.jing.common.enumeration.TripType;
import com.jing.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Where local clocks give the wrong answer: a flight that lands "earlier" than it leaves (flying west),
 * and a multi-city trip whose next journey starts at an airport in another time zone.
 */
class TimeZoneConnectionTest {

    private static final long PNH = 1, NRT = 2, LAX = 3;
    private static final ZoneId PHNOM_PENH = ZoneId.of("Asia/Phnom_Penh");    // UTC+7
    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");              // UTC+9
    private static final ZoneId LOS_ANGELES = ZoneId.of("America/Los_Angeles");
    private static final LocalDateTime DAY = LocalDateTime.now().plusDays(30).withHour(0).withMinute(0).withSecond(0).withNano(0);

    private final BookingSegmentValidator validator = new BookingSegmentValidator();

    private static BookingSegment flight(long id, int journey, long from, ZoneId fromZone, LocalDateTime dep,
                                         long to, ZoneId toZone, LocalDateTime arr) {
        return BookingSegment.builder()
                .id(id).flightInstanceId(100 + id)
                .departureAirportId(from).arrivalAirportId(to)
                .departureTime(dep).arrivalTime(arr)
                .departureAt(dep.atZone(fromZone).toInstant()).arrivalAt(arr.atZone(toZone).toInstant())
                .departureTimeZone(fromZone.getId()).arrivalTimeZone(toZone.getId())
                .journeyNumber(journey).segmentOrder((int) id)
                .build();
    }

    @Test
    void westboundFlightThatLandsEarlierOnTheClockIsValid() {
        Booking booking = Booking.builder().tripType(TripType.ONE_WAY).currency("USD").build();
        // NRT 17:00 -> LAX 10:00 the same day: 10 hours of flying, "landing before take-off" on the clocks
        BookingSegment tokyoToLa = flight(1, 1, NRT, TOKYO, DAY.withHour(17), LAX, LOS_ANGELES, DAY.withHour(10));

        assertThat(validator.validate(booking, tokyoToLa, null)).isEqualTo(1);
    }

    @Test
    void multiCityLegThatLooksLaterOnTheClockButLeavesBeforeYouLand() {
        Booking booking = Booking.builder().tripType(TripType.MULTI_CITY).currency("USD").build();
        // journey 1: NRT 08:00 -> PNH 15:00 Phnom Penh time = 08:00 UTC
        booking.addSegment(flight(1, 1, NRT, TOKYO, DAY.withHour(8), PNH, PHNOM_PENH, DAY.withHour(15)));
        // journey 2 from Tokyo at 16:00 Tokyo time = 07:00 UTC: one hour BEFORE landing in Phnom Penh.
        // Local clocks say 16:00 > 15:00 and would wrongly accept it.
        BookingSegment fromTokyo = flight(2, 2, NRT, TOKYO, DAY.withHour(16), LAX, LOS_ANGELES, DAY.withHour(9));

        assertThatThrownBy(() -> validator.validate(booking, fromTokyo, 2))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("depart after the previous segment arrives");
    }

    @Test
    void multiCityLegThatLooksEarlierOnTheClockButIsReallyLater() {
        Booking booking = Booking.builder().tripType(TripType.MULTI_CITY).currency("USD").build();
        // journey 1: PNH 08:00 -> NRT 15:00 Tokyo time = 06:00 UTC
        booking.addSegment(flight(1, 1, PNH, PHNOM_PENH, DAY.withHour(8), NRT, TOKYO, DAY.withHour(15)));
        // journey 2 from Phnom Penh at 14:00 = 07:00 UTC: after landing. Local clocks (14:00 < 15:00) would reject it.
        BookingSegment fromPhnomPenh = flight(2, 2, PNH, PHNOM_PENH, DAY.withHour(14), LAX, LOS_ANGELES, DAY.plusDays(1).withHour(9));

        assertThat(validator.validate(booking, fromPhnomPenh, 2)).isEqualTo(2);
    }
}
