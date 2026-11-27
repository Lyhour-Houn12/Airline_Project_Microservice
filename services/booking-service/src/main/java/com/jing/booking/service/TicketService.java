package com.jing.booking.service;

import com.jing.common.security.CurrentUser;
import com.jing.booking.payload.response.TicketResponse;

import java.util.List;

public interface TicketService {
    List<TicketResponse> issueForBooking(Long bookingId, CurrentUser user);

    TicketResponse getById(Long ticketId, CurrentUser user);

    TicketResponse getByTicketNumber(String ticketNumber, CurrentUser user);

    List<TicketResponse> getByBooking(Long bookingId, CurrentUser user);
    TicketResponse voidTicket(Long id, CurrentUser user);
    TicketResponse refund(Long id, CurrentUser user);
    TicketResponse markUsed(Long id, CurrentUser user);
}
