package com.jing.booking.service.impl;
import com.jing.booking.mapper.TicketMapper;
import com.jing.booking.model.Booking;
import com.jing.booking.model.BookingSegment;
import com.jing.booking.model.PassengerSegment;
import com.jing.booking.model.Ticket;
import com.jing.common.security.CurrentUser;
import com.jing.booking.payload.response.TicketResponse;
import com.jing.booking.repository.BookingRepository;
import com.jing.booking.repository.TicketRepository;
import com.jing.booking.service.TicketService;
import com.jing.booking.utils.AccessGuard;
import com.jing.booking.validatation.BookingSegmentValidator;
import com.jing.common.enumeration.BookingStatus;
import com.jing.common.enumeration.PassengerSegmentStatus;
import com.jing.common.enumeration.TicketStatus;
import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {
    private static final String AIRLINE_PREFIX = "999";   // TODO: per-airline accounting code (segment.getAirlineId())
    // SecureRandom is in java.base; RandomGenerator.getDefault() needs the jdk.random module,
    // which slim runtimes (the Docker image) don't have
    private static final SecureRandom RANDOM = new SecureRandom();

    private final TicketRepository ticketRepository;
    private final BookingRepository bookingRepository;
    private final BookingSegmentValidator segmentValidator;
    private final AccessGuard accessGuard;

    /** Admin for now; later the payment flow calls this internally and users never do. */
    @Override
    @Transactional   // findByIdForUpdate takes a lock and the segments load lazily: both need a transaction
    public List<TicketResponse> issueForBooking(Long bookingId, CurrentUser user) {
        accessGuard.requireAdmin(user, "issue ticket");

        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        if (booking.getStatus() != BookingStatus.CONFIRMED)
            throw new BadRequestException(
                    "Tickets can only be issued for a CONFIRMED booking (current: " + booking.getStatus() + ")");

        List<TicketResponse> issued = issueTickets(booking);
        if (issued.isEmpty())
            throw new BadRequestException("No passenger segments left to ticket for this booking");
        return issued;
    }

    /** One ticket per passenger per flight; already ticketed passenger segments are skipped. */
    @Override
    @Transactional
    public List<TicketResponse> issueTickets(Booking booking) {
        segmentValidator.validateComplete(booking);

        Set<Long> alreadyTicketed = ticketRepository.findByBookingId(booking.getId()).stream()
                .map(t -> t.getPassengerSegment().getId())
                .collect(Collectors.toSet());

        List<Ticket> issued = new ArrayList<>();
        for (BookingSegment segment : booking.getSegments()) {
            for (PassengerSegment ps : segment.getPassengerSegments()) {
                if (ps.getStatus() == PassengerSegmentStatus.CANCELLED) continue;
                if (alreadyTicketed.contains(ps.getId())) continue;
                issued.add(ticketRepository.save(Ticket.builder()
                        .ticketNumber(generateTicketNumber())
                        .booking(booking)
                        .passengerSegment(ps)   // belongs to this booking by construction
                        .build()));
            }
        }
        return issued.stream().map(TicketMapper::toResponse).toList();
    }

    /** Cancelling a booking voids its tickets that were not used yet. */
    @Override
    @Transactional
    public int voidIssuedTickets(Booking booking) {
        List<Ticket> issued = ticketRepository.findByBookingIdAndStatus(booking.getId(), TicketStatus.ISSUED);
        issued.forEach(Ticket::voidTicket);
        return issued.size();
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getById(Long id, CurrentUser user) {
        Ticket ticket = find(id);
        authorizeView(ticket, user);
        return TicketMapper.toResponse(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getByTicketNumber(String ticketNumber, CurrentUser user) {
        Ticket ticket = ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "ticketNumber", ticketNumber));
        authorizeView(ticket, user);
        return TicketMapper.toResponse(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> getByBooking(Long bookingId, CurrentUser user) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
        accessGuard.requireCanView(booking, user);

        return ticketRepository.findByBookingId(bookingId).stream().map(TicketMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public TicketResponse voidTicket(Long id, CurrentUser user) {
        return apply(id, user, "void tickets", Ticket::voidTicket );
    }

    @Override
    @Transactional
    public TicketResponse refund(Long id, CurrentUser user) {
        return apply(id, user, "refund tickets", Ticket::refund);
    }

    @Override
    @Transactional
    public TicketResponse markUsed(Long id, CurrentUser user) {
        return apply(id, user, "mark tickets as used", Ticket::markUsed);
    }


    private TicketResponse apply(Long id, CurrentUser user, String action, Consumer<Ticket> change){
        accessGuard.requireAdmin(user, action);

        Ticket ticket = find(id);

        try{
            change.accept(ticket);
        }catch (Exception e){
            throw new BadRequestException(e.getMessage());
        }
        return TicketMapper.toResponse(ticket);  // dirty checking flushes on commit
    }

    private void authorizeView(Ticket ticket, CurrentUser user) {
        accessGuard.requireCanView(ticket.getBooking(), user);   // 404 for other customers
    }

    private Ticket find(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", id));
    }

    private String generateTicketNumber() {
        String number;
        do{
            number = AIRLINE_PREFIX + String.format("%010d", RANDOM.nextLong(10_000_000_000L));
        }while (ticketRepository.existsByTicketNumber(number));
        return number;
    }
}
