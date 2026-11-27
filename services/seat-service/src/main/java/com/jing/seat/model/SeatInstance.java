package com.jing.seat.model;

import com.jing.common.enumeration.SeatAvailabilityStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "seat_instances",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_seat_instance_flight_seat",
                columnNames = {"flight_instance_id", "seat_id"}),
        indexes = {
                @Index(name = "idx_si_flight_status", columnList = "flight_instance_id, status"),
                @Index(name = "idx_si_cabin", columnList = "flight_instance_cabin_id")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SeatInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seat_instance_seq")
    @SequenceGenerator(name = "seat_instance_seq", sequenceName = "seat_instance_seq", allocationSize = 50)
    private Long id;

    // Cross-service ref: FlightInstance is in flight-service
    @Column(name = "flight_instance_id", nullable = false)
    private Long flightInstanceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flight_instance_cabin_id", nullable = false)
    private FlightInstanceCabin flightInstanceCabin;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SeatAvailabilityStatus status = SeatAvailabilityStatus.AVAILABLE;

    @Column(precision = 10, scale = 2)
    private BigDecimal surcharge;

    private String blockedReason;

    // Who holds or booked this seat: the booking reference (PNR). Null while AVAILABLE or BLOCKED.
    @Column(name = "hold_reference", length = 20)
    private String holdReference;

    // A HELD seat whose hold lapsed counts as free again (the booking was never paid)
    @Column(name = "held_until")
    private LocalDateTime heldUntil;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ------------------------------------------------------------------ hold life cycle

    /** True when the seat can be taken right now: AVAILABLE, or HELD by a hold that already lapsed. */
    public boolean isFreeAt(LocalDateTime now) {
        return status == SeatAvailabilityStatus.AVAILABLE
                || (status == SeatAvailabilityStatus.HELD && heldUntil != null && heldUntil.isBefore(now));
    }

    public boolean isHeldBy(String reference) {
        return status == SeatAvailabilityStatus.HELD && reference.equals(holdReference);
    }

    /** Hold the seat for a booking. Holding it again for the same booking just extends the hold. */
    public void hold(String reference, LocalDateTime until, LocalDateTime now) {
        if (!isHeldBy(reference) && !isFreeAt(now)) {
            throw new IllegalStateException("Seat " + seatLabel() + " is not available");
        }
        status = SeatAvailabilityStatus.HELD;
        holdReference = reference;
        heldUntil = until;
    }

    /** Book a seat this booking holds. Returns false when the hold was lost to someone else. */
    public boolean confirm(String reference) {
        if (!isHeldBy(reference)) {
            return status == SeatAvailabilityStatus.BOOKED && reference.equals(holdReference);
        }
        status = SeatAvailabilityStatus.BOOKED;
        heldUntil = null;
        return true;
    }

    /** Give the seat back if this booking holds or booked it. Returns the status it had before. */
    public SeatAvailabilityStatus release(String reference) {
        SeatAvailabilityStatus before = status;
        boolean mine = reference.equals(holdReference)
                && (status == SeatAvailabilityStatus.HELD || status == SeatAvailabilityStatus.BOOKED);
        if (mine) {
            status = SeatAvailabilityStatus.AVAILABLE;
            holdReference = null;
            heldUntil = null;
        }
        return mine ? before : null;
    }

    private String seatLabel() {
        return seat != null ? seat.getSeatNumber() : String.valueOf(id);
    }
}
