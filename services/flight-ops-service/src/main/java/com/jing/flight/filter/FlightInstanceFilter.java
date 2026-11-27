package com.jing.flight.filter;

import com.jing.common.enumeration.FlightStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private LocalDate onDate;           // departs on this day
    private FlightStatus status;        // ignored for customers (always SCHEDULED)
}