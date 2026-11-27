package com.jing.flight.model;

import com.jing.common.enumeration.FlightStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "flight_instances")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FlightInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "airline_id", nullable = false)
    private Long airlineId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    // Cross-service ref: Airport is in location-service
    @Column(name = "departure_airport_id", nullable = false)
    private Long departureAirportId;

    // Cross-service ref: Airport is in location-se rvice
    @Column(name = "arrival_airport_id", nullable = false)
    private Long arrivalAirportId;


    @Column(nullable = false)
    private Long scheduleId;

    @Column(nullable = false)
    private Integer totalSeats;

    @Column(nullable = false)
    private Integer availableSeats;


    @Column(name = "departure_time", nullable = false)
    private LocalDateTime departureDateTime;

    @Column(name = "arrival_time", nullable = false)
    private LocalDateTime arrivalDateTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FlightStatus status;

    private Integer minAdvanceBookingDays;
    private Integer maxAdvanceBookingDays;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;

    private String terminal;
    private String gate;

    @Version
    private Long version;

    @Transient
    public String getFormattedDuration() {
        if (departureDateTime == null || arrivalDateTime == null) {
            return null;
        }
        Duration duration = Duration.between(departureDateTime, arrivalDateTime);
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("min");
        return sb.toString().trim();
    }

}
