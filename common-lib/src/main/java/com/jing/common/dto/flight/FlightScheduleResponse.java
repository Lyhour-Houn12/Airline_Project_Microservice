package com.jing.common.dto.flight;

import com.jing.common.enumeration.RecurrenceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightScheduleResponse {
    private Long id;
    private Long flightId;
    private String flightNumber;
    private Long departureAirportId;
    private Long arrivalAirportId;
    private LocalTime departureTime;
    private LocalTime arrivalTime;
    private Integer durationMinutes;
    private LocalDate startDate;
    private LocalDate endDate;
    private Set<DayOfWeek> operatingDays;
    private RecurrenceType recurrenceType;
    private Boolean isActive;
    private Long version;
}