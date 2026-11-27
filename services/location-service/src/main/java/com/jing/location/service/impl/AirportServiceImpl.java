package com.jing.location.service.impl;

import com.jing.common.dto.location.AirportRequest;
import com.jing.common.dto.location.AirportResponse;
import com.jing.common.exception.DuplicateResourceException;
import com.jing.common.exception.ResourceNotFoundException;
import com.jing.common.pagination.PageResponse;
import com.jing.common.pagination.PageableFactory;
import com.jing.location.mapper.AirportMapper;
import com.jing.location.model.Airport;
import com.jing.location.model.City;
import com.jing.location.repository.AirportRepository;
import com.jing.location.repository.CityRepository;
import com.jing.location.service.AirportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AirportServiceImpl implements AirportService {

    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "airportName", "iataCode");

    private final AirportRepository airportRepository;
    private final CityRepository cityRepository;

    @Override
    @Transactional
    public AirportResponse createAirport(AirportRequest request) {
        String iata = normalizeIata(request.getIataCode());
        if (airportRepository.existsByIataCodeIgnoreCase(iata)) {
            throw new DuplicateResourceException("Airport", "IATA code", iata);
        }
        City city = cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new ResourceNotFoundException("City", "id", request.getCityId()));

        Airport airport = AirportMapper.toEntity(request);
        airport.setIataCode(iata);
        airport.setCity(city);
        return AirportMapper.toResponse(airportRepository.save(airport));
    }

    @Override
    @Transactional
    public List<AirportResponse> createBulkAirports(List<AirportRequest> requests) {
        List<AirportResponse> createdAirports = new ArrayList<>();
        List<String> skippedCodes = new ArrayList<>();

        for (AirportRequest request : requests) {
            String iata = normalizeIata(request.getIataCode());
            if (airportRepository.existsByIataCodeIgnoreCase(iata)) {
                skippedCodes.add(iata + " (already exists)");
                continue;
            }
            Optional<City> cityOpt = cityRepository.findById(request.getCityId());
            if (cityOpt.isEmpty()) {
                skippedCodes.add(iata + " (city not found with id: " + request.getCityId() + ")");
                continue;
            }
            Airport airport = AirportMapper.toEntity(request);
            airport.setIataCode(iata);
            airport.setCity(cityOpt.get());
            createdAirports.add(AirportMapper.toResponse(airportRepository.save(airport)));
        }

        if (!skippedCodes.isEmpty()) {
            log.info("Bulk airport creation - skipped: {}", skippedCodes);
        }
        log.info("Bulk airport creation - created {} out of {} airports", createdAirports.size(), requests.size());
        return createdAirports;
    }

    @Override
    public AirportResponse getAirportById(Long id) {
        return AirportMapper.toResponse(findOrThrow(id));
    }

    @Override
    public PageResponse<AirportResponse> getAllAirports(String keyword, int page, int size,
                                                        String sortBy, String sortDirection) {
        Pageable pageable = PageableFactory.of(page, size, sortBy, sortDirection,
                SORTABLE_FIELDS, "id", Sort.Direction.ASC);
        if (keyword == null || keyword.isBlank()) {
            return PageResponse.from(airportRepository.findAll(pageable).map(AirportMapper::toResponse));
        }
        return PageResponse.from(
                airportRepository.searchByKeyword(keyword.trim(), pageable).map(AirportMapper::toResponse));
    }

    @Override
    @Transactional
    public AirportResponse updateAirport(Long id, AirportRequest request) {
        Airport existingAirport = findOrThrow(id);

        if (request.getIataCode() != null) {
            String iata = normalizeIata(request.getIataCode());
            if (airportRepository.existsByIataCodeIgnoreCaseAndIdNot(iata, id)) {
                throw new DuplicateResourceException("Airport", "IATA code", iata);
            }
            request.setIataCode(iata);
        }
        if (request.getCityId() != null) {
            City newCity = cityRepository.findById(request.getCityId())
                    .orElseThrow(() -> new ResourceNotFoundException("City", "id", request.getCityId()));
            existingAirport.setCity(newCity);
        }

        AirportMapper.updateEntity(request, existingAirport);
        return AirportMapper.toResponse(airportRepository.save(existingAirport));
    }

    @Override
    @Transactional
    public void deleteAirport(Long id) {
        // TODO (Feign): block the delete while flights in flight-ops-service still use this airport
        airportRepository.delete(findOrThrow(id));
    }

    @Override
    public List<AirportResponse> getAirportsByCityId(Long cityId) {
        return airportRepository.findByCityId(cityId).stream()
                .map(AirportMapper::toResponse)
                .toList();
    }

    private Airport findOrThrow(Long id) {
        return airportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Airport", "id", id));
    }

    private static String normalizeIata(String iata) {
        return iata.trim().toUpperCase();
    }
}
