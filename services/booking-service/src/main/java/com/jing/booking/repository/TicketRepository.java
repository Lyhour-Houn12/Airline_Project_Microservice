package com.jing.booking.repository;

import com.jing.booking.model.Ticket;
import com.jing.common.enumeration.TicketStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByTicketNumber(String ticketNumber);

    boolean existsByTicketNumber(String ticketNumber);

    boolean existsByPassengerSegmentId(Long passengerSegmentId);


    List<Ticket> findByBookingIdAndStatus(Long bookingId, TicketStatus status);

    @EntityGraph(attributePaths = {"passengerSegment", "passengerSegment.passenger", "passengerSegment.segment"})
    List<Ticket> findByBookingId(Long bookingId);
}
