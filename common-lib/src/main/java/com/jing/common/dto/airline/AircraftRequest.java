package com.jing.common.dto.airline;

import com.jing.common.enumeration.AircraftStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AircraftRequest {

    @NotBlank(message = "Aircraft code is required")
    @Size(max = 20, message = "Aircraft code must be at most 20 characters")
    @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "Aircraft code may only contain letters, digits and '-'")
    private String code;

    @NotBlank(message = "Aircraft model is required")
    @Size(max = 50, message = "Model must be at most 50 characters")
    private String model;

    @NotBlank(message = "Manufacturer is required")
    @Size(max = 50, message = "Manufacturer must be at most 50 characters")
    private String manufacturer;

    @NotNull(message = "Seating capacity is required")
    @Positive(message = "Seating capacity must be positive")
    private Integer seatingCapacity;

    @PositiveOrZero(message = "Economy seats cannot be negative")
    private Integer economySeats;

    @PositiveOrZero(message = "Premium economy seats cannot be negative")
    private Integer premiumEconomySeats;

    @PositiveOrZero(message = "Business seats cannot be negative")
    private Integer businessSeats;

    @PositiveOrZero(message = "First class seats cannot be negative")
    private Integer firstClassSeats;

    @Positive(message = "Range must be positive")
    private Integer rangeKm;

    @Positive(message = "Cruising speed must be positive")
    private Integer cruisingSpeedKmh;

    @Positive(message = "Maximum altitude must be positive")
    private Integer maxAltitudeFt;

    @Min(value = 1900, message = "Year of manufacture must be 1900 or later")
    private Integer yearOfManufacture;

    @PastOrPresent(message = "Registration date cannot be in the future")
    private LocalDate registrationDate;

    private LocalDate nextMaintenanceDate;

    @NotNull(message = "Status is required")
    private AircraftStatus status;

    @NotNull(message = "Availability status is required")
    private Boolean isAvailable;

    private Long currentAirportId;
}