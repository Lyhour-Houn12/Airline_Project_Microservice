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

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}