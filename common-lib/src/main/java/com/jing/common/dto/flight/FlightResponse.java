package com.jing.common.dto.flight;

import com.jing.common.dto.airline.AircraftResponse;
import com.jing.common.dto.airline.AirlineResponse;
import com.jing.common.dto.location.AirportResponse;

import com.jing.common.enumeration.FlightStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FlightResponse {
    private Long id;
    private String flightNumber;
    private AirlineResponse airline;
    private AircraftResponse aircraft;
    private AirportResponse departureAirport;
    private AirportResponse arrivalAirport;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private FlightStatus status;
    private Double lowestPrice;
    private Integer totalAvailableSeats;
}
