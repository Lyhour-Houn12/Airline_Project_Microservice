package com.jing.common.dto.flight;

import com.jing.common.dto.location.AirportResponse;

import com.jing.common.enumeration.FlightStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FlightInstanceResponse {
    private Long flightId;
    private String flightNumber;

    private Long airlineId;
    private String airlineName;
    private String airlineLogo;
    private Long aircraftId;
    private String aircraftModal;
    private String aircraftCode;
    private AirportResponse departureAirport;
    private AirportResponse arrivalAirport;

    private LocalDateTime departureDateTime;
    private LocalDateTime arrivalDateTime;
    private String formattedDuration;

    private Integer totalSeats;
    private Integer availableSeats;

    private FlightStatus status;

    private Integer minAdvanceBookingDays;
    private Integer maxAdvanceBookingDays;
    private Boolean isActive;

    private String terminal;
    private String gate;

    private Long version;
}
