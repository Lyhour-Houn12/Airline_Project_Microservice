package com.jing.pricing.mapper;

import com.jing.common.dto.pricing.FareRulesRequest;
import com.jing.common.dto.pricing.FareRulesResponse;
import com.jing.pricing.model.Fare;
import com.jing.pricing.model.FareRule;

public class FareRulesMapper {

    private FareRulesMapper() {}

    public static FareRule toEntity(FareRulesRequest request, Fare fare) {
        if (request == null) return null;
        FareRule rule = new FareRule();
        rule.setFare(fare);
        rule.setAirlineId(fare.getAirlineId());   // always from the fare, never from the request
        applyFields(request, rule);
        return rule;
    }

    /** Full replace (PUT): every rule field is overwritten, so null clears it. */
    public static void updateEntity(FareRulesRequest request, FareRule existing) {
        if (request == null || existing == null) return;
        applyFields(request, existing);
    }

    private static void applyFields(FareRulesRequest request, FareRule rule) {
        rule.setRuleName(request.getRuleName());
        rule.setIsRefundable(Boolean.TRUE.equals(request.getIsRefundable()));
        rule.setIsChangeable(Boolean.TRUE.equals(request.getIsChangeable()));
        rule.setChangeFee(request.getChangeFee());
        rule.setCancellationFee(request.getCancellationFee());
        rule.setRefundableDeadlineDays(request.getRefundDeadlineDays());
        rule.setChangeDeadlineHours(request.getChangeDeadlineHours());
    }

    public static FareRulesResponse toResponse(FareRule rule) {
        if (rule == null) return null;
        return FareRulesResponse.builder()
                .id(rule.getId())
                .ruleName(rule.getRuleName())
                .fareId(rule.getFare() != null ? rule.getFare().getId() : null)
                .airlineId(rule.getAirlineId())
                .isRefundable(rule.getIsRefundable())
                .changeFee(rule.getChangeFee())
                .cancellationFee(rule.getCancellationFee())
                .refundDeadlineDays(rule.getRefundableDeadlineDays())
                .changeDeadlineHours(rule.getChangeDeadlineHours())
                .isChangeable(rule.getIsChangeable())
                .build();
    }
}
