package com.jing.booking.service;

import com.jing.booking.model.Booking;
import com.jing.common.security.CurrentUser;
import com.jing.booking.payload.response.TicketResponse;

import java.util.List;

public interface TicketService {
    /** Admin: issue the tickets that are still missing (e.g. after a failure). */
    List<TicketResponse> issueForBooking(Long bookingId, CurrentUser user);

    /** Internal: called when the booking's payment succeeds. */
    List<TicketResponse> issueTickets(Booking booking);

    /** Internal: called when the booking is cancelled. */
    int voidIssuedTickets(Booking booking);

    TicketResponse getById(Long ticketId, CurrentUser user);

    TicketResponse getByTicketNumber(String ticketNumber, CurrentUser user);

    List<TicketResponse> getByBooking(Long bookingId, CurrentUser user);
    TicketResponse voidTicket(Long id, CurrentUser user);
    TicketResponse refund(Long id, CurrentUser user);
    TicketResponse markUsed(Long id, CurrentUser user);
}
