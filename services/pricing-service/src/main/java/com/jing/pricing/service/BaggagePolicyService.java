package com.jing.pricing.service;

import com.jing.common.dto.pricing.BaggagePolicyRequest;
import com.jing.common.dto.pricing.BaggagePolicyResponse;

public interface BaggagePolicyService {

    BaggagePolicyResponse createBaggagePolicy(BaggagePolicyRequest request);

    BaggagePolicyResponse getById(Long id);

    BaggagePolicyResponse getByFareId(Long fareId);

    BaggagePolicyResponse updateBaggagePolicy(Long id, BaggagePolicyRequest request);

    void deleteBaggagePolicy(Long id);
}