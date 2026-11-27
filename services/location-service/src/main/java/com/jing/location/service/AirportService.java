package com.jing.location.service;

import com.jing.common.dto.location.AirportRequest;
import com.jing.common.dto.location.AirportResponse;
import com.jing.common.pagination.PageResponse;

import java.util.List;

public interface AirportService {

    AirportResponse createAirport(AirportRequest request);

    List<AirportResponse> createBulkAirports(List<AirportRequest> requests);

    AirportResponse getAirportById(Long id);

    PageResponse<AirportResponse> getAllAirports(String keyword, int page, int size, String sortBy, String sortDirection);

    AirportResponse updateAirport(Long id, AirportRequest request);

    void deleteAirport(Long id);

    List<AirportResponse> getAirportsByCityId(Long cityId);
}
