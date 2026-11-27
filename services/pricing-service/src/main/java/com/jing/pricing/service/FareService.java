package com.jing.pricing.service;

import com.jing.common.security.CurrentUser;
import com.jing.common.dto.pricing.FareRequest;
import com.jing.common.dto.pricing.FareResponse;
import com.jing.common.pagination.PageResponse;
import com.jing.pricing.payload.filter.FareFilter;

public interface FareService {

    FareResponse createFare(CurrentUser user, FareRequest fareRequest);

    FareResponse getById(Long id);

    FareResponse updateFare(CurrentUser user, Long id,
            FareRequest request
    );
    void deleteFare(CurrentUser user, Long id);

    PageResponse<FareResponse> getAll(FareFilter filter, int page, int size, String sortBy, String sortDirection);
}
