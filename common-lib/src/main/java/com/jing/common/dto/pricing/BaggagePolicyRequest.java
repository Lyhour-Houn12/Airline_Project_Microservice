package com.jing.common.dto.pricing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaggagePolicyRequest {


    @Size(max = 100)
    @NotBlank(message = "Policy name is required")
    private String name;

    @NotNull(message = "Fare ID is required")
    private Long fareId;


    @Size(max = 500)
    private String description;

    // Cabin baggage
    @PositiveOrZero
    private Double cabinBaggageMaxWeight;

    @PositiveOrZero
    private Integer cabinBaggagePieces;

    @PositiveOrZero
    private Double cabinBaggageWeightPerPiece;

    @PositiveOrZero
    private Double cabinBaggageMaxDimension;

    // Check-in baggage
    @PositiveOrZero
    private Double checkInBaggageMaxWeight;

    @PositiveOrZero
    private Integer checkInBaggagePieces;

    @PositiveOrZero
    private Double checkInBaggageWeightPerPiece;


    // Benefits
    private Boolean priorityBaggage;
    private Boolean extraBaggagePurchasable;

}