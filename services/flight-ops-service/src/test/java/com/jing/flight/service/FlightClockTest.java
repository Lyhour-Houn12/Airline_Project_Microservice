package com.jing.flight.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class FlightClockTest {

    private static final LocalDate DAY = LocalDate.of(2026, 12, 1);
    private static final ZoneId PHNOM_PENH = ZoneId.of("Asia/Phnom_Penh");   // UTC+7
    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");             // UTC+9
    private static final ZoneId LOS_ANGELES = ZoneId.of("America/Los_Angeles"); // UTC-8 in December

    @Test
    void sameZoneSameDay() {   // PNH 09:00 -> KOS 10:00
        assertThat(FlightClock.arrivalFor(DAY, LocalTime.of(9, 0), PHNOM_PENH, LocalTime.of(10, 0), PHNOM_PENH))
                .isEqualTo(LocalDateTime.of(2026, 12, 1, 10, 0));
    }

    @Test
    void flyingEastTheClockJumpsForward() {   // PNH 23:00 -> NRT 07:00 next day = 6 h
        LocalDateTime lands = FlightClock.arrivalFor(DAY, LocalTime.of(23, 0), PHNOM_PENH, LocalTime.of(7, 0), TOKYO);

        assertThat(lands).isEqualTo(LocalDateTime.of(2026, 12, 2, 7, 0));
        assertThat(FlightClock.flyingTime(DAY, LocalTime.of(23, 0), PHNOM_PENH, LocalTime.of(7, 0), TOKYO))
                .isEqualTo(Duration.ofHours(6));
    }

    @Test
    void flyingWestYouLandEarlierThanYouLeftOnTheClock() {   // NRT 17:00 -> LAX 10:00 the same day = 10 h
        LocalDateTime lands = FlightClock.arrivalFor(DAY, LocalTime.of(17, 0), TOKYO, LocalTime.of(10, 0), LOS_ANGELES);

        assertThat(lands).isEqualTo(LocalDateTime.of(2026, 12, 1, 10, 0));   // not the next day
        assertThat(FlightClock.flyingTime(DAY, LocalTime.of(17, 0), TOKYO, LocalTime.of(10, 0), LOS_ANGELES))
                .isEqualTo(Duration.ofHours(10));
    }

    @Test
    void sameClockTimeCanBeARealFlight() {
        // NRT 10:00 is 08:00 in Phnom Penh, so landing at PNH 10:00 is a 2-hour flight
        assertThat(FlightClock.flyingTime(DAY, LocalTime.of(10, 0), TOKYO, LocalTime.of(10, 0), PHNOM_PENH))
                .isEqualTo(Duration.ofHours(2));
        // and landing at PNH 14:00 is 6 hours
        assertThat(FlightClock.flyingTime(DAY, LocalTime.of(10, 0), TOKYO, LocalTime.of(14, 0), PHNOM_PENH))
                .isEqualTo(Duration.ofHours(6));
    }
}
