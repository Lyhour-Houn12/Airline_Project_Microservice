package com.jing.pricing.service;

import com.jing.common.security.CurrentUser;
import com.jing.common.dto.pricing.FareRulesRequest;
import com.jing.common.dto.pricing.FareRulesResponse;

public interface FareRuleService {

    FareRulesResponse createFareRule(CurrentUser user, FareRulesRequest request);

    FareRulesResponse getById(Long id);

    FareRulesResponse getByFareId(Long fareId);

    FareRulesResponse updateFareRule(CurrentUser user, Long id, FareRulesRequest request);

    void deleteFareRule(CurrentUser user, Long id);
}