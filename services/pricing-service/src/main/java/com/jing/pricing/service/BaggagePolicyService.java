package com.jing.pricing.service;

import com.jing.common.security.CurrentUser;
import com.jing.common.dto.pricing.BaggagePolicyRequest;
import com.jing.common.dto.pricing.BaggagePolicyResponse;

public interface BaggagePolicyService {

    BaggagePolicyResponse createBaggagePolicy(CurrentUser user, BaggagePolicyRequest request);

    BaggagePolicyResponse getById(Long id);

    BaggagePolicyResponse getByFareId(Long fareId);

    BaggagePolicyResponse updateBaggagePolicy(CurrentUser user, Long id, BaggagePolicyRequest request);

    void deleteBaggagePolicy(CurrentUser user, Long id);
}