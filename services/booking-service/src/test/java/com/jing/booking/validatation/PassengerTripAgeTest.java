package com.jing.booking.validatation;

import com.jing.booking.model.Booking;
import com.jing.booking.model.BookingSegment;
import com.jing.booking.model.Passenger;
import com.jing.common.enumeration.PassengerType;
import com.jing.common.enumeration.TripType;
import com.jing.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A passenger's type must be right for the whole trip, not just for today. */
class PassengerTripAgeTest {

    private final PassengerValidator validator = new PassengerValidator();
    private final LocalDate today = LocalDate.now();

    private Booking tripBetween(LocalDate outbound, LocalDate back) {
        Booking b = Booking.builder().tripType(TripType.ROUND_TRIP).currency("USD").build();
        b.addSegment(BookingSegment.builder().id(1L).departureTime(outbound.atTime(9, 0)).arrivalTime(outbound.atTime(11, 0)).build());
        b.addSegment(BookingSegment.builder().id(2L).departureTime(back.atTime(9, 0)).arrivalTime(back.atTime(11, 0)).build());
        return b;
    }

    private Passenger passenger(PassengerType type, LocalDate born, LocalDate passportExpiry) {
        return Passenger.builder().firstName("Test").lastName(type.name())
                .passengerType(type).dateOfBirth(born).passportExpiry(passportExpiry).build();
    }

    @Test
    void anInfantWhoTurnsTwoBeforeTheReturnFlightMustBeAChild() {
        LocalDate secondBirthday = today.plusMonths(2);
        Booking booking = tripBetween(today.plusMonths(1), today.plusMonths(3));   // back after the birthday
        booking.addPassenger(passenger(PassengerType.INFANT, secondBirthday.minusYears(2), null));

        assertThatThrownBy(() -> validator.revalidateForTrip(booking))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("book as a child");
    }

    @Test
    void theSameBabyIsFineAsAnInfantIfTheyAreBackBeforeTheirBirthday() {
        LocalDate secondBirthday = today.plusMonths(2);
        Booking booking = tripBetween(today.plusDays(10), today.plusDays(20));
        booking.addPassenger(passenger(PassengerType.INFANT, secondBirthday.minusYears(2), null));

        assertThatCode(() -> validator.revalidateForTrip(booking)).doesNotThrowAnyException();
    }

    @Test
    void anAdultMustAlreadyBeTwelveOnTheFirstFlight() {
        LocalDate twelfthBirthday = today.plusMonths(2);
        Booking booking = tripBetween(today.plusMonths(1), today.plusMonths(3));   // turns 12 during the trip
        booking.addPassenger(passenger(PassengerType.ADULT, twelfthBirthday.minusYears(12), null));

        assertThatThrownBy(() -> validator.revalidateForTrip(booking))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("adult must be at least 12");
    }

    @Test
    void thePassportMustStillBeValidOnTheLastFlight() {
        Booking booking = tripBetween(today.plusDays(10), today.plusDays(40));
        booking.addPassenger(passenger(PassengerType.ADULT, LocalDate.of(1990, 1, 1), today.plusDays(30)));

        assertThatThrownBy(() -> validator.revalidateForTrip(booking))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("passport expires before the last flight");
    }

}
