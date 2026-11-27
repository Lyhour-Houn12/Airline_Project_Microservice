package com.jing.booking.payload.response;

import com.jing.common.embeddable.ContactInfo;
import com.jing.common.enumeration.BookingStatus;
import com.jing.common.enumeration.TripType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        String bookingReference,
        Long userId,
        TripType tripType,
        BookingStatus status,
        BigDecimal totalAmount,
        String currency,
        boolean flexibleTicket,
        LocalDateTime expiredAt,
        LocalDateTime ticketTimeLimit,
        Long paymentId,
        ContactInfo contactInfo,
        int segmentCount,
        int passengerCount,
        LocalDateTime bookingDate,
        LocalDateTime lastModified
) {
}
