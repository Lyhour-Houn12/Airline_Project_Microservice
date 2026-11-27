package com.jing.booking.payload.response;

import com.jing.common.enumeration.Gender;
import com.jing.common.enumeration.PassengerType;

import java.time.LocalDate;

public record PassengerResponse(
        Long id,
        Long bookingId,
        PassengerType passengerType,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        Gender gender,
        String email,
        String phone,
        String passportNumberMasked,       // e.g. "******567"
        LocalDate passportExpiry,
        String passportIssuingCountry,
        String nationality,
        String frequentFlyerNumber,
        Long accompanyingAdultId,
        boolean wheelchairAssistance
) {
}
