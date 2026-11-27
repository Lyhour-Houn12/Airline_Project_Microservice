package com.jing.booking.mapper;

import com.jing.booking.model.Passenger;
import com.jing.booking.payload.request.PassengerRequest;
import com.jing.booking.payload.response.PassengerResponse;

import java.util.Locale;

public final class PassengerMapper {

    private PassengerMapper() {}

    public static Passenger toEntity(PassengerRequest r, Passenger accompanyingAdult) {
        Passenger p = new Passenger();
        apply(p, r, accompanyingAdult);
        return p;
    }

    /** Copies the request onto the entity. A null passportNumber keeps the current one. */
    public static void apply(Passenger p, PassengerRequest r, Passenger accompanyingAdult) {
        p.setPassengerType(r.passengerType());
        p.setFirstName(cleanName(r.firstName()));
        p.setLastName(cleanName(r.lastName()));
        p.setDateOfBirth(r.dateOfBirth());
        p.setGender(r.gender());
        p.setEmail(blankToNull(r.email()));
        p.setPhone(blankToNull(r.phone()));
        if (r.passportNumber() != null) {
            p.setPassportNumber(normalizePassport(r.passportNumber()));
        }
        p.setPassportExpiry(r.passportExpiry());
        p.setPassportIssuingCountry(upperOrNull(r.passportIssuingCountry()));
        p.setNationality(upperOrNull(r.nationality()));
        p.setFrequentFlyerNumber(blankToNull(r.frequentFlyerNumber()));
        p.setAccompanyingAdult(accompanyingAdult);
        p.setWheelchairAssistance(r.wheelchairAssistance());
    }

    public static PassengerResponse toResponse(Passenger p) {
        return new PassengerResponse(
                p.getId(),
                p.getBooking().getId(),
                p.getPassengerType(),
                p.getFirstName(),
                p.getLastName(),
                p.getDateOfBirth(),
                p.getGender(),
                p.getEmail(),
                p.getPhone(),
                maskPassport(p.getPassportNumber()),
                p.getPassportExpiry(),
                p.getPassportIssuingCountry(),
                p.getNationality(),
                p.getFrequentFlyerNumber(),
                p.getAccompanyingAdult() != null ? p.getAccompanyingAdult().getId() : null,
                p.isWheelchairAssistance());
    }

    public static String normalizePassport(String raw) {
        return raw == null ? null : raw.trim().toUpperCase(Locale.ROOT);
    }

    public static String maskPassport(String passport) {
        if (passport == null || passport.isBlank()) return null;
        int n = passport.length();
        if (n <= 4) return "*".repeat(n);
        return "*".repeat(n - 3) + passport.substring(n - 3);
    }

    // names are kept as entered (they must match the passport): only trim and collapse spaces
    private static String cleanName(String s) { return s.trim().replaceAll("\\s+", " "); }
    private static String blankToNull(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }
    private static String upperOrNull(String s) { return (s == null || s.isBlank()) ? null : s.trim().toUpperCase(Locale.ROOT); }

}
