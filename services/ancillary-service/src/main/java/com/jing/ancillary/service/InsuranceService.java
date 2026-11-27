package com.jing.ancillary.service;

import com.jing.ancillary.filter.InsuranceCoverageFilter;
import com.jing.common.dto.ancillary.InsuranceCoverageRequest;
import com.jing.common.dto.ancillary.InsuranceCoverageResponse;
import com.jing.common.pagination.PageResponse;

public interface InsuranceService {
    InsuranceCoverageResponse createCoverage(InsuranceCoverageRequest request);

    InsuranceCoverageResponse getById(Long id);

    PageResponse<InsuranceCoverageResponse> getAll(InsuranceCoverageFilter filter, int page, int size,
                                                   String sortBy, String sortDirection);

    InsuranceCoverageResponse updateCoverage(Long id, InsuranceCoverageRequest request);

    InsuranceCoverageResponse changeStatus(Long id, boolean active);

    void deleteCoverage(Long id);
}
