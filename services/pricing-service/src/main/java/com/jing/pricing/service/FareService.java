package com.jing.pricing.service;

import com.jing.common.dto.pricing.FareRequest;
import com.jing.common.dto.pricing.FareResponse;
import com.jing.common.pagination.PageResponse;
import com.jing.pricing.payload.filter.FareFilter;

public interface FareService {

    FareResponse createFare(FareRequest fareRequest);

    FareResponse getById(Long id);

    FareResponse updateFare(
            Long id,
            FareRequest request
    );
    void deleteFare(Long id);

    PageResponse<FareResponse> getAll(FareFilter filter, int page, int size, String sortBy, String sortDirection);
}
