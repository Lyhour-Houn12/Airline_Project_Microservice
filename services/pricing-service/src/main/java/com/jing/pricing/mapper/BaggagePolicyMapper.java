package com.jing.pricing.mapper;

import com.jing.common.dto.pricing.BaggagePolicyRequest;
import com.jing.common.dto.pricing.BaggagePolicyResponse;
import com.jing.pricing.model.BaggagePolicy;
import com.jing.pricing.model.Fare;

public class BaggagePolicyMapper {

    private BaggagePolicyMapper() {}

    public static BaggagePolicy toEntity(BaggagePolicyRequest request, Fare fare) {
        if (request == null) return null;
        BaggagePolicy policy = new BaggagePolicy();
        policy.setFare(fare);
        policy.setAirlineId(fare.getAirlineId());   // from the fare, not the request
        applyFields(request, policy);
        return policy;
    }

    /** Full replace (PUT): every field is overwritten, so null clears it. */
    public static void updateEntity(BaggagePolicyRequest request, BaggagePolicy existing) {
        if (request == null || existing == null) return;
        applyFields(request, existing);
    }

    private static void applyFields(BaggagePolicyRequest r, BaggagePolicy p) {
        p.setName(r.getName());
        p.setDescription(r.getDescription());

        p.setCabinBaggageMaxWeight(r.getCabinBaggageMaxWeight());
        p.setCabinBaggagePieces(r.getCabinBaggagePieces() != null ? r.getCabinBaggagePieces() : 1);
        p.setCabinBaggageWeightPerPiece(r.getCabinBaggageWeightPerPiece());
        p.setCabinBaggageMaxDimension(r.getCabinBaggageMaxDimension());

        p.setCheckInBaggageMaxWeight(r.getCheckInBaggageMaxWeight());
        p.setCheckInBaggagePieces(r.getCheckInBaggagePieces() != null ? r.getCheckInBaggagePieces() : 0);
        p.setCheckInBaggageWeightPerPiece(r.getCheckInBaggageWeightPerPiece());


        p.setPriorityBaggage(Boolean.TRUE.equals(r.getPriorityBaggage()));
        p.setExtraBaggagePurchasable(Boolean.TRUE.equals(r.getExtraBaggagePurchasable()));
    }

    public static BaggagePolicyResponse toResponse(BaggagePolicy policy) {
        if (policy == null) return null;
        return BaggagePolicyResponse.builder()
                .id(policy.getId())
                .name(policy.getName())
                .description(policy.getDescription())
                .cabinBaggageMaxWeight(policy.getCabinBaggageMaxWeight())
                .cabinBaggagePieces(policy.getCabinBaggagePieces())
                .cabinBaggageWeightPerPiece(policy.getCabinBaggageWeightPerPiece())
                .cabinBaggageMaxDimension(policy.getCabinBaggageMaxDimension())
                .checkInBaggageMaxWeight(policy.getCheckInBaggageMaxWeight())
                .checkInBaggagePieces(policy.getCheckInBaggagePieces())
                .checkInBaggageWeightPerPiece(policy.getCheckInBaggageWeightPerPiece())
                .priorityBaggage(policy.getPriorityBaggage())
                .extraBaggagePurchasable(policy.getExtraBaggagePurchasable())
                .airlineId(policy.getAirlineId())
                .fareId(policy.getFare() != null ? policy.getFare().getId() : null)
                .build();
    }
}