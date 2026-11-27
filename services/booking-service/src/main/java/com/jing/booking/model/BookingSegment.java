package com.jing.booking.model;

import com.jing.common.enumeration.CabinClassType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "booking_segments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"booking_id", "segment_order"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingSegment {

    @Id
    @GeneratedValue(strategy =  GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Column(name = "segment_order", nullable = false)
    private int segmentOrder;

    // cross service refs
    @Column(nullable = false)
    private Long flightId;
    @Column(nullable = false)
    private Long flightInstanceId;
    @Column(nullable = false)
    private Long airlineId;
    @Column(nullable = false)
    private Long fareId;
    @Column(nullable = false)
    private Long cabinClassId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CabinClassType cabinClass;

    // Snapshot of what was sold (so later schedule changes don't rewrite history)
    @Column(nullable = false) private Long departureAirportId;
    @Column(nullable = false) private Long arrivalAirportId;
    @Column(nullable = false) private LocalDateTime departureTime;
    @Column(nullable = false) private LocalDateTime arrivalTime;

    // The same moments in UTC, plus the zones, copied from flight-ops. Compare THESE: local times of two
    // airports in different zones can't be compared. Null only on segments booked before time zones.
    private Instant departureAt;
    private Instant arrivalAt;
    @Column(length = 40) private String departureTimeZone;
    @Column(length = 40) private String arrivalTimeZone;

    /** Departure on the UTC clock (old segments: their local time read in the server's zone). */
    public Instant departsAt() {
        return departureAt != null ? departureAt : departureTime.atZone(ZoneId.systemDefault()).toInstant();
    }

    public Instant arrivesAt() {
        return arrivalAt != null ? arrivalAt : arrivalTime.atZone(ZoneId.systemDefault()).toInstant();
    }
    private String flightNumber;

    @Column(name = "journey_number", nullable = false)
    private int journeyNumber;

    // Fare snapshot (one adult) taken from pricing-service when the segment was added.
    // Passengers added later are priced from this, so a later fare change can't alter the booking.
    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal baseFare = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal taxesAndFees = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal airlineFees = BigDecimal.ZERO;

    @OneToMany(mappedBy = "segment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PassengerSegment> passengerSegments = new ArrayList<>();

    @Override public boolean equals(Object o) {
        return this == o || (o instanceof BookingSegment b && id != null && id.equals(b.id));
    }
    @Override public int hashCode() { return getClass().hashCode(); }

}
