package com.jing.booking.model;


import com.jing.common.enumeration.TicketStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 14)
    private String ticketNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TicketStatus status = TicketStatus.ISSUED;

    private LocalDateTime issuedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    // One ticket per passenger per segment
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "passenger_segment_id", unique = true)
    private PassengerSegment passengerSegment;

    @PrePersist
    void onCreate(){
        if (issuedAt == null){
            issuedAt = LocalDateTime.now();
        }
    }

    // ---- state transitions: only ISSUED tickets can change ----
    public void voidTicket(){
        transitionTo(TicketStatus.VOIDED);
    }
    public void refund(){
        transitionTo(TicketStatus.REFUNDED);
    }
    public void markUsed()    { transitionTo(TicketStatus.USED); }

    private void transitionTo(TicketStatus target){
        if (this.status != TicketStatus.ISSUED){
            throw new IllegalStateException("Ticket " + ticketNumber + " is " + status + " and cannot become " + target);
        }
        this.status = target;
    }

}
