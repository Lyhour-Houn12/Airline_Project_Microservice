package com.jing.flight.filter;

import com.jing.common.enumeration.FlightStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightInstanceFilter {
    private String keyword;             // matches the flight number
    private Long airlineId;
    private Long flightId;
    private Long departureAirportId;
    private Long arrivalAirportId;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate onDate;           // departs on this day (local date at the departure airport), e.g. 2026-12-24
    private FlightStatus status;        // ignored for customers (always SCHEDULED)
}