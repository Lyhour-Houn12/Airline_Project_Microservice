package com.jing.common.dto.pricing;

import com.jing.common.enumeration.CabinClassType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FareRequest {

    @NotBlank(message = "Fare name is required")
    @Size(max = 255, message = "Fare name must be at most 255 characters")
    private String fareName;

    @NotNull(message = "RBD code is required")
    private Character rbdCode;

    @NotNull(message = "Airline ID is required")
    private Long airlineId;

    @NotNull(message = "Flight ID is required")
    private Long flightId;

    @NotNull(message = "Cabin class ID is required")
    private Long cabinClassId;

    @NotNull(message = "Cabin class is required")
    private CabinClassType cabinClass;

    // Pricing
    @NotNull(message = "Base fare is required")
    @Positive(message = "Base fare must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Base fare must have at most 2 decimal places")
    private BigDecimal baseFare;

    @PositiveOrZero(message = "Taxes and fees cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Taxes and fees must have at most 2 decimal places")
    private BigDecimal taxesAndFees;

    @PositiveOrZero(message = "Airline fees cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Airline fees must have at most 2 decimal places")
    private BigDecimal airlineFees;

    /** Optional override (promo / dynamic price). Leave null to use base + taxes + fees. */
    @Positive(message = "Current price must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Current price must have at most 2 decimal places")
    private BigDecimal currentPrice;

    /** ISO 4217 code, e.g. USD, KHR. Defaults to USD when omitted. */
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code, e.g. USD")
    private String currency;

    @Size(max = 100, message = "Fare label must be at most 100 characters")
    private String fareLabel;

    // Seat benefits
    private Boolean extraSeatSpace;
    private Boolean preferredSeatChoice;
    private Boolean advanceSeatSelection;
    private Boolean guaranteeSeatTogether;

    // Boarding benefits
    private Boolean priorityBoarding;
    private Boolean priorityCheckIn;
    private Boolean fastTrackSecurity;

    // In-flight benefits
    private Boolean complimentaryMeals;
    private Boolean premiumMealChoice;
    private Boolean inFlightInternet;
    private Boolean inFlightEntertainment;
    private Boolean complimentaryBeverages;

    // Flexibility benefits
    private Boolean freeDateChange;
    private Boolean partialRefund;
    private Boolean fullRefund;

    // Premium service benefits
    private Boolean loungeAccess;
    private Boolean airportTransfer;
}