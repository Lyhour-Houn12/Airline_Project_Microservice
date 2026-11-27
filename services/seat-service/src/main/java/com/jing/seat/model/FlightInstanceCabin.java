package com.jing.seat.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "flight_instance_cabins",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_flight_instance_cabin",
                columnNames = {"flight_instance_id", "cabin_class_id"}))
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FlightInstanceCabin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Cross-service ref: FlightInstance lives in flight-service
    @Column(name = "flight_instance_id", nullable = false, updatable = false)
    private Long flightInstanceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cabin_class_id", nullable = false, updatable = false)
    private CabinClass cabinClass;

    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;

    // Places of PAID bookings (CabinReservation BOOKED).
    @Builder.Default
    @Column(name = "booked_seats", nullable = false)
    private Integer bookedSeats = 0;

    // Places of UNPAID bookings (CabinReservation HELD). Nullable column so existing rows get added.
    @Builder.Default
    @Column(name = "held_seats")
    private Integer heldSeats = 0;

    @Builder.Default
    @Column(name = "is_bookable", nullable = false)
    private Boolean isBookable = true;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public int getHeldSeats() {
        return heldSeats != null ? heldSeats : 0;
    }

    /** Free places: neither paid for nor held by an unpaid booking. Can be negative if overbooked. */
    public int getAvailableSeats() {
        return totalSeats - bookedSeats - getHeldSeats();
    }

    // ------------------------------------------------------------ capacity bookkeeping

    /** An unpaid booking's places change by {@code delta} (negative = given back). */
    public void changeHeld(int delta) {
        heldSeats = Math.max(0, getHeldSeats() + delta);
    }

    /** {@code seats} held places of a booking were paid for. */
    public void heldToBooked(int seats) {
        changeHeld(-seats);
        bookedSeats = bookedSeats + seats;
    }

    public void changeBooked(int delta) {
        bookedSeats = Math.max(0, bookedSeats + delta);
    }
}
