package com.jing.common.dto.flight;

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
public class FlightInstanceRequest {
    @NotNull(message = "Flight ID is required")
    private Long flightId;

    private Long airlineId;

    private Long scheduleId;            // optional: leave empty for a one-off flight (extra, charter, replacement)

    private Long departureAirportId;

    private Long arrivalAirportId;

    @NotNull(message = "Departure date-time is required")
    private LocalDateTime departureDateTime;

    @NotNull(message = "Arrival date-time is required")
    private LocalDateTime arrivalDateTime;

    @NotNull(message = "Total seats is required")
    @Positive
    private Integer totalSeats;

    @PositiveOrZero
    private Integer availableSeats;

    private FlightStatus status;

    private Integer minAdvanceBookingDays;
    private Integer maxAdvanceBookingDays;
    private Boolean isActive;

    private String terminal;
    private String gate;
}
