package com.jing.booking.payload.request;

import com.jing.common.embeddable.ContactInfo;
import com.jing.common.enumeration.TripType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateDraftBookingRequest(
        @NotNull TripType tripType,
        @NotBlank @Size(min = 3, max = 3) String currency,
        boolean flexibleTicket,
        ContactInfo contactInfo
        ) {
}
