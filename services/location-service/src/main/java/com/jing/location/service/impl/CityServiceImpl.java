package com.jing.location.service.impl;

import com.jing.common.dto.location.CityRequest;
import com.jing.common.dto.location.CityResponse;
import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.DuplicateResourceException;
import com.jing.common.exception.ResourceNotFoundException;
import com.jing.common.pagination.PageResponse;
import com.jing.common.pagination.PageableFactory;
import com.jing.location.mapper.CityMapper;
import com.jing.location.model.City;
import com.jing.location.repository.AirportRepository;
import com.jing.location.repository.CityRepository;
import com.jing.location.service.CityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CityServiceImpl implements CityService {

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "cityName", "cityCode", "countryName", "createdAt");

    private final CityRepository cityRepository;
    private final AirportRepository airportRepository;

    @Override
    @Transactional
    public CityResponse createCity(CityRequest request) {
        normalize(request);
        validateCityRequest(request);
        if (cityRepository.existsByCityCode(request.getCityCode())) {
            throw new DuplicateResourceException("City", "cityCode", request.getCityCode());
        }
        return CityMapper.toResponse(cityRepository.save(CityMapper.toEntity(request)));
    }

    @Override
    @Transactional
    public List<CityResponse> createBulkCities(List<CityRequest> requests) {
        List<CityResponse> createdCities = new ArrayList<>();
        List<String> skippedCodes = new ArrayList<>();

        for (CityRequest request : requests) {
            normalize(request);
            try {
                validateCityRequest(request);
            } catch (BadRequestException e) {
                skippedCodes.add(request.getCityCode() + " (invalid: " + e.getMessage() + ")");
                continue;
            }
            if (cityRepository.existsByCityCode(request.getCityCode())) {
                skippedCodes.add(request.getCityCode() + " (already exists)");
                continue;
            }
            createdCities.add(CityMapper.toResponse(cityRepository.save(CityMapper.toEntity(request))));
        }

        if (!skippedCodes.isEmpty()) {
            log.info("Bulk city creation - skipped: {}", skippedCodes);
        }
        log.info("Bulk city creation - created {} out of {} cities", createdCities.size(), requests.size());
        return createdCities;
    }

    @Override
    public CityResponse getCityById(Long id) {
        return CityMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public CityResponse updateCity(Long id, CityRequest request) {
        City city = findOrThrow(id);
        normalize(request);
        validateCityRequest(request);
        if (cityRepository.existsByCityCodeAndIdNot(request.getCityCode(), id)) {
            throw new DuplicateResourceException("City", "cityCode", request.getCityCode());
        }
        return CityMapper.toResponse(cityRepository.save(CityMapper.cityUpdate(city, request)));
    }

    @Override
    @Transactional
    public void deleteCity(Long id) {
        City city = findOrThrow(id);
        if (airportRepository.existsByCityId(id)) {
            throw new BadRequestException("This city still has airports. Delete or move them first");
        }
        cityRepository.delete(city);
    }

    @Override
    public PageResponse<CityResponse> getAllCities(int page, int size, String sortBy, String sortDirection) {
        return PageResponse.from(cityRepository.findAll(page(page, size, sortBy, sortDirection))
                .map(CityMapper::toResponse));
    }

    @Override
    public PageResponse<CityResponse> searchCities(String keyword, int page, int size,
                                                   String sortBy, String sortDirection) {
        Pageable pageable = page(page, size, sortBy, sortDirection);
        if (keyword == null || keyword.isBlank()) {
            return PageResponse.from(cityRepository.findAll(pageable).map(CityMapper::toResponse));
        }
        return PageResponse.from(
                cityRepository.searchByKeyword(keyword.trim(), pageable).map(CityMapper::toResponse));
    }

    @Override
    public PageResponse<CityResponse> getCitiesByCountryCode(String countryCode, Pageable pageable) {
        return PageResponse.from(
                cityRepository.findByCountryCodeIgnoreCase(countryCode, pageable).map(CityMapper::toResponse));
    }

    @Override
    public boolean cityExists(String cityCode) {
        return cityRepository.existsByCityCode(cityCode);
    }

    @Override
    public boolean validateCityCode(String cityCode) {
        return cityCode != null && cityCode.matches("[A-Z0-9]{2,10}");
    }

    private City findOrThrow(Long id) {
        return cityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("City", "id", id));
    }

    /** Codes are stored upper-case, so "pnh" and "PNH" are the same city. */
    private static void normalize(CityRequest r) {
        if (r.getCityCode() != null) r.setCityCode(r.getCityCode().trim().toUpperCase());
        if (r.getCountryCode() != null) r.setCountryCode(r.getCountryCode().trim().toUpperCase());
        if (r.getRegionCode() != null) r.setRegionCode(r.getRegionCode().trim().toUpperCase());
        if (r.getCityName() != null) r.setCityName(r.getCityName().trim());
        if (r.getCountryName() != null) r.setCountryName(r.getCountryName().trim());
    }

    private void validateCityRequest(CityRequest request) {
        if (!validateCityCode(request.getCityCode())) {
            throw new BadRequestException("Invalid city code format. Must be 2-10 letters or digits");
        }
        if (request.getCountryCode() == null || !request.getCountryCode().matches("[A-Z]{2,5}")) {
            throw new BadRequestException("Country code must be 2-5 letters");
        }
        if (request.getTimeZoneOffset() != null && !request.getTimeZoneOffset().matches("[+-]\\d{2}:\\d{2}")) {
            throw new BadRequestException("Time zone offset must be in format ±HH:MM");
        }
    }

    private static Pageable page(int page, int size, String sortBy, String sortDirection) {
        return PageableFactory.of(page, size, sortBy, sortDirection,
                SORTABLE_FIELDS, "cityName", Sort.Direction.ASC);
    }
}
