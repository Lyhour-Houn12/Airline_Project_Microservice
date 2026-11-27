package com.jing.booking.mapper;

import com.jing.booking.model.Ticket;
import com.jing.booking.payload.response.TicketResponse;

public final class TicketMapper {

    public static TicketResponse toResponse(Ticket t){
        return new TicketResponse(
                t.getId(),
                t.getTicketNumber(),
                t.getStatus(),
                t.getIssuedAt(),
                t.getBooking().getId(),
                t.getPassengerSegment().getId()
        );
    }
}
