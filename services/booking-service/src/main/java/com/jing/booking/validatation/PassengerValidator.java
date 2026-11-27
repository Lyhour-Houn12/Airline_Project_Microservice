package com.jing.booking.validatation;

import com.jing.booking.mapper.PassengerMapper;
import com.jing.booking.model.Booking;
import com.jing.booking.model.BookingSegment;
import com.jing.booking.model.Passenger;
import com.jing.booking.payload.request.PassengerRequest;
import com.jing.common.enumeration.PassengerType;
import com.jing.common.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.time.Period;

@Component
public class PassengerValidator {

    private static final int ADULT_MIN_AGE = 12;
    private static final int CHILD_MIN_AGE = 2;


    /**
     * Validates a passenger against the booking. 'existing' is null when adding.
     * Returns the accompanying adult for an infant, or null.
     */
    public Passenger validate(PassengerRequest r, Booking booking, Passenger existing) {
        Trip trip = Trip.of(booking);
        checkAge(r.passengerType(), r.dateOfBirth(), trip, null);
        checkPassport(r.passportExpiry(), trip, null);
        validateNotDuplicate(r, booking, existing);

        if (existing != null && r.passengerType() != PassengerType.ADULT
                && hostsInfant(booking, existing)) {
            throw new BadRequestException(
                    "This passenger travels with an infant and must stay an adult. Remove or change the infant first");
        }
        return resolveAccompanyingAdult(r, booking, existing);
    }

    public boolean hostsInfant(Booking booking, Passenger adult) {
        return booking.getPassengers().stream()
                .anyMatch(p -> p.getAccompanyingAdult() != null && p.getAccompanyingAdult().getId().equals(adult.getId()));
    }


    /**
     * The travel dates that matter: a passenger's type must hold for the whole trip, so minimum ages are
     * checked on the first flight and maximum ages (and the passport) on the last one. Local dates at the
     * departure airports. Without flights yet: today.
     */
    record Trip(LocalDate firstDay, LocalDate lastDay) {
        static Trip of(Booking booking) {
            List<BookingSegment> segments = booking.getSegments();
            if (segments.isEmpty()) {
                LocalDate today = LocalDate.now();
                return new Trip(today, today);
            }
            LocalDate first = segments.stream().map(s -> s.getDepartureTime().toLocalDate()).min(LocalDate::compareTo).get();
            LocalDate last = segments.stream().map(s -> s.getDepartureTime().toLocalDate()).max(LocalDate::compareTo).get();
            return new Trip(first, last);
        }
    }

    /**
     * Re-checks every passenger after the booking's flights changed: a baby who was an infant for a
     * flight next week may already be 2 on a return flight next year.
     */
    public void revalidateForTrip(Booking booking) {
        Trip trip = Trip.of(booking);
        for (Passenger p : booking.getPassengers()) {
            checkAge(p.getPassengerType(), p.getDateOfBirth(), trip, p.getFullName());
            checkPassport(p.getPassportExpiry(), trip, p.getFullName());
        }
    }

    private void checkAge(PassengerType type, LocalDate dateOfBirth, Trip trip, String who) {
        int ageAtStart = Period.between(dateOfBirth, trip.firstDay()).getYears();
        int ageAtEnd = Period.between(dateOfBirth, trip.lastDay()).getYears();
        String prefix = who == null ? "" : who + ": ";
        switch (type) {
            case ADULT -> {
                if (ageAtStart < ADULT_MIN_AGE)
                    throw new BadRequestException(prefix + "an adult must be at least " + ADULT_MIN_AGE
                            + " years old on " + trip.firstDay());
            }
            case CHILD -> {
                if (ageAtStart < CHILD_MIN_AGE)
                    throw new BadRequestException(prefix + "a child must be at least " + CHILD_MIN_AGE
                            + " years old on " + trip.firstDay() + " (book as an infant)");
                if (ageAtEnd >= ADULT_MIN_AGE)
                    throw new BadRequestException(prefix + "is " + ADULT_MIN_AGE + " or older on " + trip.lastDay()
                            + " and must be booked as an adult");
            }
            case INFANT -> {
                if (ageAtEnd >= CHILD_MIN_AGE)
                    throw new BadRequestException(prefix + "is " + CHILD_MIN_AGE + " or older on " + trip.lastDay()
                            + " and needs a seat: book as a child");
            }
        }
    }

    /** The passport must still be valid on the day of the last flight. */
    private void checkPassport(LocalDate passportExpiry, Trip trip, String who) {
        // many countries also require 6 months validity on arrival; that depends on the destination
        if (passportExpiry != null && !passportExpiry.isAfter(trip.lastDay())) {
            throw new BadRequestException((who == null ? "" : who + ": ")
                    + "passport expires before the last flight (" + trip.lastDay() + ")");
        }
    }

    private void validateNotDuplicate(PassengerRequest r, Booking booking, Passenger existing){
        String passport = r.passportNumber() != null
                ? PassengerMapper.normalizePassport(r.passportNumber())
                : (existing != null ? existing.getPassportNumber() : null);

        String first = r.firstName().trim();
        String last = r.lastName().trim();

        for (Passenger other: booking.getPassengers()){
            if (existing != null && other == existing){
                continue;
            }
            if (passport != null && passport.equalsIgnoreCase(other.getPassportNumber())) {
                throw new BadRequestException("Another passenger on this booking has the same passport number");
            }
            if (first.equalsIgnoreCase(other.getFirstName())
                    && last.equalsIgnoreCase(other.getLastName())
                    && r.dateOfBirth().equals(other.getDateOfBirth())) {
                throw new BadRequestException("This passenger is already on the booking");
            }
        }
    }

    private Passenger resolveAccompanyingAdult(PassengerRequest r, Booking booking, Passenger existing) {
        if (r.passengerType() != PassengerType.INFANT) {
            if (r.accompanyingAdultId() != null) {
                throw new BadRequestException("Only an infant can have an accompanying adult");
            }
            return null;
        }
        if (r.accompanyingAdultId() == null) {
            throw new BadRequestException("An infant must name the adult who travels with them (accompanyingAdultId)");
        }

        Passenger adult = booking.getPassengers().stream()
                .filter(p -> p.getId() != null && p.getId().equals(r.accompanyingAdultId()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "The accompanying adult must be a passenger on this booking"));

        if (adult.getPassengerType() != PassengerType.ADULT) {
            throw new BadRequestException("The accompanying passenger must be an adult");
        }
        if (existing != null && adult == existing) {
            throw new BadRequestException("A passenger cannot accompany themselves");
        }
        boolean alreadyHosting = booking.getPassengers().stream()
                .anyMatch(p -> p != existing
                        && p.getAccompanyingAdult() != null
                        && p.getAccompanyingAdult().getId().equals(adult.getId()));
        if (alreadyHosting) {
            throw new BadRequestException("This adult already travels with an infant");
        }
        return adult;
    }
}
