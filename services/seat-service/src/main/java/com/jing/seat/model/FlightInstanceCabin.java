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

    @Builder.Default
    @Column(name = "booked_seats", nullable = false)
    private Integer bookedSeats = 0;

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

    public int getAvailableSeats() {
        return totalSeats - bookedSeats;
    }
}