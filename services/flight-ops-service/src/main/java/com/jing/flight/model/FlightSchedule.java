package com.jing.flight.model;


import com.jing.common.enumeration.RecurrenceType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "flight_schedules")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class FlightSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch =  FetchType.LAZY, optional = false)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    // Cross-service ref: Airport is in location-service
    @Column(name = "departure_airport_id", nullable = false)
    private Long departureAirportId;

    // Cross-service ref: Airport is in location-service
    @Column(name = "arrival_airport_id", nullable = false)
    private Long arrivalAirportId;

    @Column(nullable = false)
    private LocalTime departureTime;
    @Column(nullable = false)
    private LocalTime arrivalTime;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;


    @ElementCollection
    @CollectionTable(name = "schedule_operating_days", joinColumns = @JoinColumn(name = "schedule_id"))
    @Column(name = "day_of_week")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<DayOfWeek> operatingDays = new HashSet<>();

    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;

    @Enumerated(EnumType.STRING)

    private RecurrenceType recurrenceType;

    @Version
    private Long version;
}
