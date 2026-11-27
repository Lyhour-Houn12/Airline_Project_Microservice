package com.jing.flight.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Time arithmetic for schedules, whose departure and arrival times are each in their own airport's zone.
 */
public final class FlightClock {

    private FlightClock() {}

    /**
     * The local arrival date-time of a scheduled flight that departs on {@code day}: the first time the
     * arrival clock shows {@code arrival} after the departure moment. Handles arriving "earlier" on the
     * clock when flying west, and arriving the next day.
     */
    public static LocalDateTime arrivalFor(LocalDate day, LocalTime departure, ZoneId departureZone,
                                           LocalTime arrival, ZoneId arrivalZone) {
        ZonedDateTime departs = ZonedDateTime.of(day, departure, departureZone);
        ZonedDateTime arrives = ZonedDateTime.of(day, arrival, arrivalZone);
        while (!arrives.isAfter(departs)) {
            arrives = arrives.plusDays(1);
        }
        while (arrives.minusDays(1).isAfter(departs)) {   // landed on an earlier local day (flying far west)
            arrives = arrives.minusDays(1);
        }
        return arrives.toLocalDateTime();
    }

    /** Real flying time of a schedule on a given day (can differ around daylight-saving changes). */
    public static Duration flyingTime(LocalDate day, LocalTime departure, ZoneId departureZone,
                                      LocalTime arrival, ZoneId arrivalZone) {
        LocalDateTime lands = arrivalFor(day, departure, departureZone, arrival, arrivalZone);
        return Duration.between(ZonedDateTime.of(day, departure, departureZone), lands.atZone(arrivalZone));
    }
}
