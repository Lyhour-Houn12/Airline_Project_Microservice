package com.jing.common.dto.flight;

import com.jing.common.enumeration.RecurrenceType;
import jakarta.validation.constraints.NotNull;
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
public class FlightScheduleRequest {
    @NotNull(message = "Flight ID is required")
    private Long flightId;
    private Long departureAirportId;   // defaults to the flight's
    private Long arrivalAirportId;     // defaults to the flight's
    @NotNull(message = "Departure time is required")
    private LocalTime departureTime;
    @NotNull(message = "Arrival time is required")
    private LocalTime arrivalTime;
    @NotNull(message = "Start date is required")
    private LocalDate startDate;
    @NotNull(message = "End date is required")
    private LocalDate endDate;
    private Set<DayOfWeek> operatingDays;   // empty = every day
    private RecurrenceType recurrenceType;
    private Boolean isActive;
}