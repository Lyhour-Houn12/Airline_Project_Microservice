package com.jing.booking.controller;

import com.jing.booking.payload.response.TicketResponse;
import com.jing.booking.service.TicketService;
import com.jing.common.security.CurrentUser;
import com.jing.common.security.RequireRole;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.jing.common.enumeration.UserRole.ROLE_SYSTEM_ADMIN;

/**
 * Tickets are issued automatically when a booking is paid. Customers read their own;
 * the changes (void / refund / used, or re-issuing missing tickets) are admin tools.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @GetMapping("/bookings/{bookingId}/tickets")
    public List<TicketResponse> byBooking(CurrentUser user, @PathVariable Long bookingId) {
        return ticketService.getByBooking(bookingId, user);
    }

    @PostMapping("/bookings/{bookingId}/tickets")
    @RequireRole(ROLE_SYSTEM_ADMIN)
    public ResponseEntity<List<TicketResponse>> issueMissing(CurrentUser user, @PathVariable Long bookingId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.issueForBooking(bookingId, user));
    }

    @GetMapping("/tickets/{id}")
    public TicketResponse getById(CurrentUser user, @PathVariable Long id) {
        return ticketService.getById(id, user);
    }

    @GetMapping("/tickets/number/{ticketNumber}")
    public TicketResponse getByNumber(CurrentUser user, @PathVariable String ticketNumber) {
        return ticketService.getByTicketNumber(ticketNumber, user);
    }

    @PatchMapping("/tickets/{id}/void")
    @RequireRole(ROLE_SYSTEM_ADMIN)
    public TicketResponse voidTicket(CurrentUser user, @PathVariable Long id) {
        return ticketService.voidTicket(id, user);
    }

    @PatchMapping("/tickets/{id}/refund")
    @RequireRole(ROLE_SYSTEM_ADMIN)
    public TicketResponse refund(CurrentUser user, @PathVariable Long id) {
        return ticketService.refund(id, user);
    }

    @PatchMapping("/tickets/{id}/use")
    @RequireRole(ROLE_SYSTEM_ADMIN)
    public TicketResponse markUsed(CurrentUser user, @PathVariable Long id) {
        return ticketService.markUsed(id, user);
    }
}
