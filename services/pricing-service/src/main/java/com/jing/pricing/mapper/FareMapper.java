package com.jing.pricing.mapper;

import com.jing.common.embeddable.*;
import com.jing.common.dto.pricing.FareRequest;
import com.jing.common.dto.pricing.FareResponse;
import com.jing.pricing.model.Fare;

public class FareMapper {

    private static final String DEFAULT_CURRENCY = "USD";

    private FareMapper() {
    }


    public static Fare toEntity(FareRequest request) {
        if (request == null) return null;

        Fare fare = Fare.builder()
                .fareName(request.getFareName())
                .rbdCode(request.getRbdCode())
                .airlineId(request.getAirlineId())
                .flightId(request.getFlightId())
                .cabinClassId(request.getCabinClassId())
                .cabinClass(request.getCabinClass())
                .baseFare(request.getBaseFare())
                .taxesAndFees(request.getTaxesAndFees())
                .airlineFees(request.getAirlineFees())
                .currentPrice(request.getCurrentPrice())
                .currency(request.getCurrency() != null ? request.getCurrency() : DEFAULT_CURRENCY)
                .fareLabel(request.getFareLabel())
                .build();

        applyBenefits(request, fare);
        return fare;
    }


    public static FareResponse toResponse(Fare fare) {
        if (fare == null) return null;

        SeatBenefits sb = fare.getSeatBenefits();
        BoardingBenefits bb = fare.getBoardingBenefits();
        InFlightBenefits ifb = fare.getInFlightBenefits();
        FlexibilityBenefits fb = fare.getFlexibilityBenefits();
        PremiumServiceBenefits psb = fare.getPremiumServiceBenefits();

        return FareResponse.builder()
                .id(fare.getId())
                .fareName(fare.getFareName())
                .rbdCode(fare.getRbdCode())
                .airlineId(fare.getAirlineId())
                .flightId(fare.getFlightId())
                .cabinClassId(fare.getCabinClassId())
                .cabinClass(fare.getCabinClass())

                .baseFare(fare.getBaseFare())
                .taxesAndFees(fare.getTaxesAndFees())
                .airlineFees(fare.getAirlineFees())
                .currentPrice(fare.getCurrentPrice())
                .totalPrice(fare.getTotalPrice())
                .effectivePrice(fare.getEffectivePrice())
                .currency(fare.getCurrency())
                .fareLabel(fare.getFareLabel())
                .fareRulesId(fare.getFareRule() != null ? fare.getFareRule().getId() : null)

                // Seat benefits
                .extraSeatSpace(sb != null && isTrue(sb.getExtraSeatSpace()))
                .preferredSeatChoice(sb != null && isTrue(sb.getPreferredSeatChoice()))
                .advanceSeatSelection(sb != null && isTrue(sb.getAdvanceSeatSelection()))
                .guaranteeSeatTogether(sb != null && isTrue(sb.getGuaranteeSeatTogether()))

                // Boarding benefits
                .priorityBoarding(bb != null && isTrue(bb.getPriorityBoarding()))
                .priorityCheckIn(bb != null && isTrue(bb.getPriorityCheckIn()))
                .fastTrackSecurity(bb != null && isTrue(bb.getFastTrackSecurity()))

                // In-flight benefits
                .complimentaryMeals(ifb != null && isTrue(ifb.getComplimentaryMeals()))
                .premiumMealChoice(ifb != null && isTrue(ifb.getPremiumMealChoice()))
                .inFlightInternet(ifb != null && isTrue(ifb.getInFlightInternet()))
                .inFlightEntertainment(ifb != null && isTrue(ifb.getInFlightEntertainment()))
                .complimentaryBeverages(ifb != null && isTrue(ifb.getComplimentaryBeverages()))

                // Flexibility benefits
                .freeDateChange(fb != null && isTrue(fb.getFreeDateChange()))
                .partialRefund(fb != null && isTrue(fb.getPartialRefund()))
                .fullRefund(fb != null && isTrue(fb.getFullRefund()))

                // Premium service benefits
                .loungeAccess(psb != null && isTrue(psb.getLoungeAccess()))
                .airportTransfer(psb != null && isTrue(psb.getAirportTransfer()))

                // Enable once these mappers exist
                .fareRules(fare.getFareRule() != null ? FareRulesMapper.toResponse(fare.getFareRule()) : null)
                .baggagePolicy(fare.getBaggagePolicy() != null ? BaggagePolicyMapper.toResponse(fare.getBaggagePolicy()) : null)
                .build();
    }

    // ---------------------------------------------------------------- update (partial)

    public static void updateEntity(FareRequest request, Fare existing) {
        if (request == null || existing == null) return;

        if (request.getFareName() != null) existing.setFareName(request.getFareName());
        if (request.getRbdCode() != null) existing.setRbdCode(request.getRbdCode());
        if (request.getAirlineId() != null) existing.setAirlineId(request.getAirlineId());
        if (request.getFlightId() != null) existing.setFlightId(request.getFlightId());
        if (request.getCabinClassId() != null) existing.setCabinClassId(request.getCabinClassId());
        if (request.getCabinClass() != null) existing.setCabinClass(request.getCabinClass());

        if (request.getBaseFare() != null) existing.setBaseFare(request.getBaseFare());
        if (request.getTaxesAndFees() != null) existing.setTaxesAndFees(request.getTaxesAndFees());
        if (request.getAirlineFees() != null) existing.setAirlineFees(request.getAirlineFees());
        if (request.getCurrentPrice() != null) existing.setCurrentPrice(request.getCurrentPrice());
        if (request.getCurrency() != null) existing.setCurrency(request.getCurrency());
        if (request.getFareLabel() != null) existing.setFareLabel(request.getFareLabel());

        applyBenefits(request, existing);
    }


    /**
     * Applies only the benefit flags present (non-null) in the request.
     * Creates any embeddable Hibernate loaded as null, so it is safe for
     * both new and existing fares.
     */
    private static void applyBenefits(FareRequest r, Fare fare) {
        if (fare.getSeatBenefits() == null) fare.setSeatBenefits(new SeatBenefits());
        if (fare.getBoardingBenefits() == null) fare.setBoardingBenefits(new BoardingBenefits());
        if (fare.getInFlightBenefits() == null) fare.setInFlightBenefits(new InFlightBenefits());
        if (fare.getFlexibilityBenefits() == null) fare.setFlexibilityBenefits(new FlexibilityBenefits());
        if (fare.getPremiumServiceBenefits() == null) fare.setPremiumServiceBenefits(new PremiumServiceBenefits());

        SeatBenefits sb = fare.getSeatBenefits();
        sb.setExtraSeatSpace(pick(r.getExtraSeatSpace(), sb.getExtraSeatSpace()));
        sb.setPreferredSeatChoice(pick(r.getPreferredSeatChoice(), sb.getPreferredSeatChoice()));
        sb.setAdvanceSeatSelection(pick(r.getAdvanceSeatSelection(), sb.getAdvanceSeatSelection()));
        sb.setGuaranteeSeatTogether(pick(r.getGuaranteeSeatTogether(), sb.getGuaranteeSeatTogether()));

        BoardingBenefits bb = fare.getBoardingBenefits();
        bb.setPriorityBoarding(pick(r.getPriorityBoarding(), bb.getPriorityBoarding()));
        bb.setPriorityCheckIn(pick(r.getPriorityCheckIn(), bb.getPriorityCheckIn()));
        bb.setFastTrackSecurity(pick(r.getFastTrackSecurity(), bb.getFastTrackSecurity()));

        InFlightBenefits ifb = fare.getInFlightBenefits();
        ifb.setComplimentaryMeals(pick(r.getComplimentaryMeals(), ifb.getComplimentaryMeals()));
        ifb.setPremiumMealChoice(pick(r.getPremiumMealChoice(), ifb.getPremiumMealChoice()));
        ifb.setInFlightInternet(pick(r.getInFlightInternet(), ifb.getInFlightInternet()));
        ifb.setInFlightEntertainment(pick(r.getInFlightEntertainment(), ifb.getInFlightEntertainment()));
        ifb.setComplimentaryBeverages(pick(r.getComplimentaryBeverages(), ifb.getComplimentaryBeverages()));

        FlexibilityBenefits fb = fare.getFlexibilityBenefits();
        fb.setFreeDateChange(pick(r.getFreeDateChange(), fb.getFreeDateChange()));
        fb.setPartialRefund(pick(r.getPartialRefund(), fb.getPartialRefund()));
        fb.setFullRefund(pick(r.getFullRefund(), fb.getFullRefund()));

        PremiumServiceBenefits psb = fare.getPremiumServiceBenefits();
        psb.setLoungeAccess(pick(r.getLoungeAccess(), psb.getLoungeAccess()));
        psb.setAirportTransfer(pick(r.getAirportTransfer(), psb.getAirportTransfer()));
    }

    private static Boolean pick(Boolean requested, Boolean current) {
        if (requested != null) return requested;
        return current != null ? current : Boolean.FALSE;
    }

    private static boolean isTrue(Boolean value) {
        return Boolean.TRUE.equals(value);
    }
}