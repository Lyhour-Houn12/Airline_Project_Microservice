package com.jing.booking.validatation;

import com.jing.booking.mapper.PassengerMapper;
import com.jing.booking.model.Booking;
import com.jing.booking.model.Passenger;
import com.jing.booking.payload.request.PassengerRequest;
import com.jing.common.enumeration.PassengerType;
import com.jing.common.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
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
        // TODO: use the departure date of the last segment once segments exist
        LocalDate reference = LocalDate.now();

        validateAge(r, reference);
        validatePassportExpiry(r, reference);
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


    private void validateAge(PassengerRequest r, LocalDate reference){
        int age = Period.between(r.dateOfBirth(), LocalDate.now()).getYears();

        switch (r.passengerType()){
            case ADULT -> {
                if (age < ADULT_MIN_AGE){
                    throw new BadRequestException("An adult must be at least " + ADULT_MIN_AGE + " years old");
                }
            }
            case CHILD -> {
                if (age < CHILD_MIN_AGE || age >= ADULT_MIN_AGE) {
                    throw new BadRequestException("A child must be between " + CHILD_MIN_AGE
                            + " and " + (ADULT_MIN_AGE - 1) + " years old");
                }
            }
            case INFANT -> {
                if (age >= CHILD_MIN_AGE) {
                    throw new BadRequestException("An infant must be under " + CHILD_MIN_AGE + " years old");
                }
            }
        }
    }

    private void validatePassportExpiry(PassengerRequest r, LocalDate reference) {
        // TODO: many countries require 6 months validity after the return date: check once segments exist
        if (r.passportExpiry() != null && !r.passportExpiry().isAfter(reference)) {
            throw new BadRequestException("Passport has expired");
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
