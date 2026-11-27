package com.jing.pricing.service;

import com.jing.common.dto.pricing.FareRulesRequest;
import com.jing.common.dto.pricing.FareRulesResponse;

public interface FareRuleService {

    FareRulesResponse createFareRule(FareRulesRequest request);

    FareRulesResponse getById(Long id);

    FareRulesResponse getByFareId(Long fareId);

    FareRulesResponse updateFareRule(Long id, FareRulesRequest request);

    void deleteFareRule(Long id);
}