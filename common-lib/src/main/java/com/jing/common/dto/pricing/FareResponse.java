package com.jing.common.dto.pricing;

import com.jing.common.enumeration.CabinClassType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FareResponse {

    private Long id;
    private String fareName;
    private Character rbdCode;
    private Long airlineId;
    private Long flightId;
    private Long cabinClassId;
    private CabinClassType cabinClass;

    // Pricing
    private BigDecimal baseFare;
    private BigDecimal taxesAndFees;
    private BigDecimal airlineFees;
    private BigDecimal currentPrice;    // override, may be null
    private BigDecimal totalPrice;      // base + taxes + fees
    private BigDecimal effectivePrice;  // currentPrice if set, otherwise totalPrice
    private String currency;
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

    // Relationships
    private Long fareRulesId;
    private FareRulesResponse fareRules;
    private BaggagePolicyResponse baggagePolicy;
}