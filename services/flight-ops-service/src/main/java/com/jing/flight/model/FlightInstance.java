package com.jing.flight.model;

import com.jing.common.enumeration.FlightStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

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


    // the schedule that generated this flight; null for a one-off flight (extra, charter, replacement)
    private Long scheduleId;

    @Column(nullable = false)
    private Integer totalSeats;

    @Column(nullable = false)
    private Integer availableSeats;


    // Local wall-clock times: departure in the departure airport's zone, arrival in the arrival airport's zone.
    // That's what is printed on a ticket ("dep 10:00 NRT, arr 03:00 LAX").
    @Column(name = "departure_time", nullable = false)
    private LocalDateTime departureDateTime;

    @Column(name = "arrival_time", nullable = false)
    private LocalDateTime arrivalDateTime;

    // The same two moments on one global clock (UTC), worked out from the airports' time zones.
    // Use these to compare times: "already departed?", "arrives before departing?", connections.
    // Null only for rows created before time zones existed (FlightTimeZoneBackfill fills them in).
    @Column(name = "departure_at")
    private Instant departureAt;

    @Column(name = "arrival_at")
    private Instant arrivalAt;

    @Column(name = "departure_time_zone", length = 40)
    private String departureTimeZone;

    @Column(name = "arrival_time_zone", length = 40)
    private String arrivalTimeZone;

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

    /** Works out the UTC moments from the local times and the two airports' zones. */
    public void applyTimeZones(ZoneId departureZone, ZoneId arrivalZone) {
        this.departureTimeZone = departureZone.getId();
        this.arrivalTimeZone = arrivalZone.getId();
        this.departureAt = departureDateTime == null ? null : departureDateTime.atZone(departureZone).toInstant();
        this.arrivalAt = arrivalDateTime == null ? null : arrivalDateTime.atZone(arrivalZone).toInstant();
    }

    @Transient
    public String getFormattedDuration() {
        if (departureDateTime == null || arrivalDateTime == null) {
            return null;
        }
        // real flying time: across time zones the local clocks alone give the wrong answer
        Duration duration = departureAt != null && arrivalAt != null
                ? Duration.between(departureAt, arrivalAt)
                : Duration.between(departureDateTime, arrivalDateTime);
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("min");
        return sb.toString().trim();
    }

}
