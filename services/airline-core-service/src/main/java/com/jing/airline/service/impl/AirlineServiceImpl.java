package com.jing.airline.service.impl;

import com.jing.airline.mapper.AirlineMapper;
import com.jing.airline.model.Airline;
import com.jing.airline.repository.AircraftRepository;
import com.jing.airline.repository.AirlineRepository;
import com.jing.airline.service.AirlineService;
import com.jing.common.dto.airline.AirlineDropDownItem;
import com.jing.common.dto.airline.AirlineRequest;
import com.jing.common.dto.airline.AirlineResponse;
import com.jing.common.enumeration.AirlineStatus;
import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.DuplicateResourceException;
import com.jing.common.exception.ForbiddenException;
import com.jing.common.exception.ResourceNotFoundException;
import com.jing.common.pagination.PageResponse;
import com.jing.common.pagination.PageableFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AirlineServiceImpl implements AirlineService {

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "airlineName", "iataCode", "icaoCode", "country", "status", "createdAt");

    private final AirlineRepository airlineRepository;
    private final AircraftRepository aircraftRepository;

    @Override
    @Transactional
    public AirlineResponse createAirline(AirlineRequest request, Long ownerId) {
        if (airlineRepository.existsByOwnerId(ownerId)) {
            throw new DuplicateResourceException("Airline", "ownerId", ownerId);
        }
        assertCodesAvailable(request.getIataCode(), request.getIcaoCode(), null);

        Airline airline = AirlineMapper.toEntity(request, ownerId);
        airline.setStatus(ownerStatus(request.getStatus(), AirlineStatus.ACTIVE));
        airline.setIataCode(airline.getIataCode().toUpperCase());
        airline.setIcaoCode(airline.getIcaoCode().toUpperCase());
        airline.setUpdatedById(ownerId);

        return AirlineMapper.toResponse(airlineRepository.save(airline));
    }

    @Override
    public AirlineResponse getAirlineByOwner(Long ownerId) {
        return AirlineMapper.toResponse(findByOwnerId(ownerId));
    }

    @Override
    public AirlineResponse getAirlineById(Long id) {
        return AirlineMapper.toResponse(findOrThrow(id));
    }

    @Override
    public PageResponse<AirlineResponse> getAllAirlines(String keyword, int page, int size,
                                                        String sortBy, String sortDirection) {
        String q = keyword == null ? "" : keyword.trim();
        var pageable = PageableFactory.of(page, size, sortBy, sortDirection,
                SORTABLE_FIELDS, "airlineName", Sort.Direction.ASC);
        Page<AirlineResponse> result = airlineRepository.search(q, pageable).map(AirlineMapper::toResponse);
        return PageResponse.from(result);
    }

    @Override
    @Transactional
    public AirlineResponse updateAirline(AirlineRequest request, Long ownerId) {
        Airline airline = findByOwnerId(ownerId);
        assertCodesAvailable(request.getIataCode(), request.getIcaoCode(), airline.getId());

        // a BANNED airline stays banned until an admin lifts it
        if (airline.getStatus() == AirlineStatus.BANNED
                && request.getStatus() != null && request.getStatus() != AirlineStatus.BANNED) {
            throw new ForbiddenException("This airline is banned; only a system admin can change its status");
        }
        AirlineStatus status = airline.getStatus() == AirlineStatus.BANNED
                ? AirlineStatus.BANNED
                : ownerStatus(request.getStatus(), airline.getStatus());

        AirlineMapper.updateEntity(airline, request);
        airline.setStatus(status);
        airline.setIataCode(airline.getIataCode().toUpperCase());
        airline.setIcaoCode(airline.getIcaoCode().toUpperCase());
        airline.setUpdatedById(ownerId);

        return AirlineMapper.toResponse(airline);
    }

    @Override
    @Transactional
    public void deleteAirline(Long id, Long ownerId) {
        Airline airline = findOrThrow(id);
        if (!airline.getOwnerId().equals(ownerId)) {
            throw new ForbiddenException("You do not own this airline");
        }
        if (aircraftRepository.existsByAirlineId(id)) {
            throw new BadRequestException("This airline still has aircraft. Delete them first");
        }
        airlineRepository.delete(airline);
    }

    @Override
    @Transactional
    public AirlineResponse changeStatusByAdmin(Long airlineId, AirlineStatus status) {
        Airline airline = findOrThrow(airlineId);
        airline.setStatus(status);
        return AirlineMapper.toResponse(airline);
    }

    @Override
    public List<AirlineDropDownItem> getAirlinesForDropdown() {
        return airlineRepository.findDropdownItems(AirlineStatus.ACTIVE);
    }

    /** Owners may switch between ACTIVE and INACTIVE; BANNED is set by admins only. */
    private AirlineStatus ownerStatus(AirlineStatus requested, AirlineStatus fallback) {
        if (requested == null) {
            return fallback;
        }
        if (requested == AirlineStatus.BANNED) {
            throw new ForbiddenException("Only a system admin can ban an airline");
        }
        return requested;
    }

    private void assertCodesAvailable(String iata, String icao, Long excludeId) {
        boolean iataTaken = excludeId == null
                ? airlineRepository.existsByIataCodeIgnoreCase(iata)
                : airlineRepository.existsByIataCodeIgnoreCaseAndIdNot(iata, excludeId);
        if (iataTaken) throw new DuplicateResourceException("Airline", "iataCode", iata);

        boolean icaoTaken = excludeId == null
                ? airlineRepository.existsByIcaoCodeIgnoreCase(icao)
                : airlineRepository.existsByIcaoCodeIgnoreCaseAndIdNot(icao, excludeId);
        if (icaoTaken) throw new DuplicateResourceException("Airline", "icaoCode", icao);
    }

    private Airline findOrThrow(Long id) {
        return airlineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Airline", "id", id));
    }

    private Airline findByOwnerId(Long ownerId) {
        return airlineRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Airline", "ownerId", ownerId));
    }
}
