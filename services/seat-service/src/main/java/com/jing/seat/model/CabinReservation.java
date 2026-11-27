package com.jing.seat.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * The places one booking takes in one cabin of one flight, whether or not its passengers pick seats.
 * This is what stops overselling: a cabin can't hand out more places than it has.
 * HELD while the booking is unpaid (until {@code heldUntil}), BOOKED once it's paid.
 */
@Entity
@Table(name = "cabin_reservations",
        uniqueConstraints = @UniqueConstraint(name = "uk_cabin_reservation",
                columnNames = {"flight_instance_cabin_id", "hold_reference"}),
        indexes = @Index(name = "idx_cabin_reservation_reference", columnList = "hold_reference"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CabinReservation {

    public enum Status { HELD, BOOKED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flight_instance_cabin_id", nullable = false)
    private FlightInstanceCabin flightInstanceCabin;

    /** The booking reference (PNR). */
    @Column(name = "hold_reference", nullable = false, length = 20)
    private String holdReference;

    @Column(nullable = false)
    private int seats;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private Status status = Status.HELD;

    private LocalDateTime heldUntil;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public boolean isLapsed(LocalDateTime now) {
        return status == Status.HELD && heldUntil != null && heldUntil.isBefore(now);
    }
}
