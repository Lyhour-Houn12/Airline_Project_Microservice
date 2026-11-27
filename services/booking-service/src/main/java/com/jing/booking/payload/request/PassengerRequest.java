package com.jing.booking.payload.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jing.common.enumeration.Gender;
import com.jing.common.enumeration.PassengerType;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record PassengerRequest(
        @NotNull PassengerType passengerType,

        @NotBlank @Size(max = 50)
        @Pattern(regexp = "^\\p{L}[\\p{L} '\\-]*$", message = "must contain only letters, spaces, hyphens or apostrophes")
        String firstName,

        @NotBlank @Size(max = 50)
        @Pattern(regexp = "^\\p{L}[\\p{L} '\\-]*$", message = "must contain only letters, spaces, hyphens or apostrophes")
        String lastName,
        @NotNull @Past  @JsonFormat(pattern = "dd/MM/yyyy") LocalDate dateOfBirth,
        @NotNull Gender gender,
        @Email @Size(max = 100) String email,
        @Size(max = 20) String phone,

        // optional for now; becomes required for international flights once flights are connected
        // on update: omit it to keep the current number
        @Size(min = 6, max = 20) @Pattern(regexp = "^[A-Za-z0-9]+$", message = "must be letters and digits only")
        String passportNumber,
        @Future @JsonFormat(pattern = "dd/MM/yyyy") LocalDate passportExpiry,
        @Size(min = 2, max = 3) String passportIssuingCountry,
        @Size(min = 2, max = 3) String nationality,
        @Size(max = 30) String frequentFlyerNumber,

        Long accompanyingAdultId,          // required for INFANT, must be null otherwise
        Boolean wheelchairAssistance
        ) {
        public PassengerRequest {
                if (wheelchairAssistance == null) wheelchairAssistance = false;
        }
}
