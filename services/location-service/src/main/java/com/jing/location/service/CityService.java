package com.jing.location.service;

import com.jing.common.dto.location.CityRequest;
import com.jing.common.dto.location.CityResponse;
import com.jing.common.pagination.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CityService {

    CityResponse createCity(CityRequest request);

    List<CityResponse> createBulkCities(List<CityRequest> requests);

    CityResponse getCityById(Long id);

    CityResponse updateCity(Long id, CityRequest request);

    void deleteCity(Long id);

    PageResponse<CityResponse> getAllCities(int page, int size, String sortBy, String sortDirection);

    PageResponse<CityResponse> searchCities(String keyword, int page, int size, String sortBy, String sortDirection);

    PageResponse<CityResponse> getCitiesByCountryCode(String countryCode, Pageable pageable);

    boolean cityExists(String cityCode);
    boolean validateCityCode(String cityCode);
}
