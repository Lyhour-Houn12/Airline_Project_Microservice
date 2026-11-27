package com.jing.common.dto.pricing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaggagePolicyResponse {

    private Long id;
    private String name;
    private String description;

    // Cabin baggage
    private Double cabinBaggageMaxWeight;
    private Integer cabinBaggagePieces;
    private Double cabinBaggageWeightPerPiece;
    private Double cabinBaggageMaxDimension;

    // Check-in baggage
    private Double checkInBaggageMaxWeight;
    private Integer checkInBaggagePieces;
    private Double checkInBaggageWeightPerPiece;

    // Benefits
    private Boolean priorityBaggage;
    private Boolean extraBaggagePurchasable;

    // Relationships
    private Long airlineId;
    private Long fareId;


}
