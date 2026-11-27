package com.jing.booking.model;

import com.jing.common.enumeration.PassengerSegmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "passenger_segments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"passenger_id", "segment_id"}),
        indexes = @Index(columnList = "segment_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PassengerSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "passenger_id")
    private Passenger passenger;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "segment_id")
    private BookingSegment segment;

    // cross-service ref: SeatInstance (seat-service). Null until a seat is assigned; infants never get one
    private Long seatInstanceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PassengerSegmentStatus status = PassengerSegmentStatus.NOT_SEATED;

    // Fare snapshot per passenger per segment (filled from pricing-service once Feign is wired)
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal baseFare = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal taxesAndFees = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal airlineFees = BigDecimal.ZERO;

    // Extra charged by seat-service for the chosen seat (exit row, front rows...). 0 without a seat.
    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal seatSurcharge = BigDecimal.ZERO;

    public BigDecimal getTotalPrice() {
        BigDecimal surcharge = seatSurcharge != null ? seatSurcharge : BigDecimal.ZERO;
        return baseFare.add(taxesAndFees).add(airlineFees).add(surcharge);
    }

    @Override public boolean equals(Object o) {
        return this == o || (o instanceof PassengerSegment p && id != null && id.equals(p.id));
    }
    @Override public int hashCode() { return getClass().hashCode(); }
}