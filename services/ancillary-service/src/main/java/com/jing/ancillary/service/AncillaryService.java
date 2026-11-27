package com.jing.ancillary.service;

import com.jing.ancillary.filter.AncillaryFilter;
import com.jing.common.dto.ancillary.AncillaryRequest;
import com.jing.common.dto.ancillary.AncillaryResponse;
import com.jing.common.pagination.PageResponse;

public interface AncillaryService {

    AncillaryResponse createAncillary(AncillaryRequest request);

    AncillaryResponse getById(Long id);

    PageResponse<AncillaryResponse> getAll(AncillaryFilter filter, int page, int size,
                                           String sortBy, String sortDirection);

    AncillaryResponse updateAncillary(Long id, AncillaryRequest request);

    AncillaryResponse changeStatus(Long id, boolean active);

    void deleteAncillary(Long id);
}
