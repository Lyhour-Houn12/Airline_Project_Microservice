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

import java.time.Instant;
import java.time.LocalDateTime;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FlightInstanceResponse {
    private Long id;
    private Long flightId;
    private String flightNumber;

    private Long airlineId;
    private String airlineName;
    private String airlineLogo;
    private Long aircraftId;
    private String aircraftModal;
    private String aircraftCode;
    private Long departureAirportId;
    private Long arrivalAirportId;
    private AirportResponse departureAirport;
    private AirportResponse arrivalAirport;

    /** Local time at the departure airport, as printed on the ticket. */
    private LocalDateTime departureDateTime;
    /** Local time at the arrival airport. */
    private LocalDateTime arrivalDateTime;
    /** The same moments in UTC: compare these, never the local times. Null for not-yet-converted old rows. */
    private Instant departureAt;
    private Instant arrivalAt;
    private String departureTimeZone;   // e.g. "Asia/Phnom_Penh"
    private String arrivalTimeZone;
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
