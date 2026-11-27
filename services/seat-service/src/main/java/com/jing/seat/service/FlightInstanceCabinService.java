package com.jing.seat.service;

import com.jing.common.dto.seat.FlightInstanceCabinBulkRequest;
import com.jing.seat.filter.FlightInstanceCabinFilter;
import com.jing.common.dto.seat.FlightInstanceCabinUpdateRequest;
import com.jing.common.dto.seat.FlightInstanceCabinResponse;
import com.jing.common.pagination.PageResponse;

import java.util.List;

public interface FlightInstanceCabinService {
    List<FlightInstanceCabinResponse> createCabins(FlightInstanceCabinBulkRequest request);
    FlightInstanceCabinResponse getById(Long id);
    List<FlightInstanceCabinResponse> getByFlightInstanceId(Long flightInstanceId);
    PageResponse<FlightInstanceCabinResponse> search(FlightInstanceCabinFilter filter, int page, int size,
                                                     String sortBy, String sortDirection);
    FlightInstanceCabinResponse update(Long id, FlightInstanceCabinUpdateRequest request);
    void delete(Long id);
}