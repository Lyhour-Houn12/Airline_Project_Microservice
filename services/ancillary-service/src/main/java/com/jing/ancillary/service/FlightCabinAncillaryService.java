package com.jing.ancillary.service;

import com.jing.ancillary.filter.FlightCabinAncillaryFilter;
import com.jing.common.dto.ancillary.FlightCabinAncillaryBulkRequest;
import com.jing.common.dto.ancillary.FlightCabinAncillaryUpdateRequest;
import com.jing.common.dto.ancillary.FlightCabinAncillaryResponse;
import com.jing.common.pagination.PageResponse;

import java.util.List;

public interface FlightCabinAncillaryService {
    List<FlightCabinAncillaryResponse> createFlightCabinAncillaries(FlightCabinAncillaryBulkRequest request);

    FlightCabinAncillaryResponse getById(Long id);

    PageResponse<FlightCabinAncillaryResponse> getAll(FlightCabinAncillaryFilter filter, int page, int size,
                                                      String sortBy, String sortDirection);

    FlightCabinAncillaryResponse updateFlightCabinAncillary(Long id, FlightCabinAncillaryUpdateRequest request);

    FlightCabinAncillaryResponse changeStatus(Long id, boolean active);

    void deleteFlightCabinAncillary(Long id);
}
