package com.jing.flight.model;

import com.jing.common.enumeration.FlightStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "flights")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Flight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String flightNumber;

    @Column(name = "airline_id", nullable = false)
    private Long airlineId;

    @Column(name = "aircraft_id", nullable = false)
    private Long aircraftId;

    // Cross-service ref: Airport is in location-service
    @Column(name = "departure_airport_id", nullable = false)
    private Long departureAirportId;

    // Cross-service ref: Airport is in location-service
    @Column(name = "arrival_airport_id")
    private Long arrivalAirportId;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FlightStatus status;


    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;


    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
