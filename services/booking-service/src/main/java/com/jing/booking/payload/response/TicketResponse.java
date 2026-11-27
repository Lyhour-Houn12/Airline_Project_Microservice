package com.jing.booking.payload.response;

import com.jing.common.enumeration.TicketStatus;

import java.time.LocalDateTime;

public record TicketResponse(
        Long id,
        String ticketNumber,
        TicketStatus status,
        LocalDateTime issuedAt,
        Long bookingId,
        Long passengerSegmentId
) {
}
