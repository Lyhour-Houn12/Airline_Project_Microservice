package com.jing.airline.service.impl;

import com.jing.airline.mapper.AircraftMapper;
import com.jing.airline.model.Aircraft;
import com.jing.airline.model.Airline;
import com.jing.airline.repository.AircraftRepository;
import com.jing.airline.repository.AirlineRepository;
import com.jing.airline.service.AircraftService;
import com.jing.common.enumeration.AircraftStatus;
import com.jing.common.enumeration.AirlineStatus;
import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.DuplicateResourceException;
import com.jing.common.exception.ForbiddenException;
import com.jing.common.pagination.PageableFactory;
import com.jing.common.exception.ResourceNotFoundException;
import com.jing.common.dto.airline.AircraftRequest;
import com.jing.common.dto.airline.AircraftResponse;
import com.jing.common.pagination.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AircraftServiceImpl implements AircraftService {
    private final AircraftRepository aircraftRepository;
    private final AirlineRepository airlineRepository;

    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "code", "model", "manufacturer", "seatingCapacity",
            "yearOfManufacture", "status", "createdAt", "id");


    @Override
    public AircraftResponse getAircraftById(Long id) {
        Aircraft aircraft = aircraftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aircraft", "id", id));
        return AircraftMapper.toResponse(aircraft);
    }

    @Override
    public List<AircraftResponse> listAllAircraftByOwner(Long ownerId) {
        return aircraftRepository.findAllByAirlineOwnerIdOrderByCodeAsc(ownerId).stream()
                .map(AircraftMapper::toResponse)
                .toList();
    }
    @Override
    @Transactional
    public AircraftResponse createAircraft(AircraftRequest request, Long ownerId) {
        Airline airline = airlineRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Airline", "ownerId", ownerId));

        if (airline.getStatus() != AirlineStatus.ACTIVE) {
            throw new BadRequestException("Aircraft can only be added to an ACTIVE airline");
        }

        if (request.getStatus() == AircraftStatus.BANNED) {
            throw new ForbiddenException("Only a system admin can ban an aircraft");
        }
        validateBusinessRules(request);

        String code = normalizeCode(request.getCode());
        if (aircraftRepository.existsByCodeIgnoreCase(code)) {
            throw new DuplicateResourceException("Aircraft", "code", code);
        }

        Aircraft aircraft = AircraftMapper.toEntity(request, airline);
        aircraft.setCode(code);

        return AircraftMapper.toResponse(aircraftRepository.save(aircraft));
    }

    @Override
    @Transactional
    public AircraftResponse updateAircraft(Long id, AircraftRequest request, Long ownerId) {
        Aircraft aircraft = findOwnedOrThrow(id, ownerId);

        // BANNED is set and lifted by admins only
        boolean banned = aircraft.getStatus() == AircraftStatus.BANNED;
        if (banned != (request.getStatus() == AircraftStatus.BANNED)) {
            throw new ForbiddenException(banned
                    ? "This aircraft is banned; only a system admin can change its status"
                    : "Only a system admin can ban an aircraft");
        }
        validateBusinessRules(request);

        String code = normalizeCode(request.getCode());
        if (aircraftRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new DuplicateResourceException("Aircraft", "code", code);
        }

        AircraftMapper.updateEntity(aircraft, request, aircraft.getAirline());
        aircraft.setCode(code);

        return AircraftMapper.toResponse(aircraft);
    }


    @Override
    @Transactional
    public void deleteAircraft(Long id, Long ownerId) {
        Aircraft aircraft = findOwnedOrThrow(id, ownerId);
        aircraftRepository.delete(aircraft);
    }

    @Override
    public PageResponse<AircraftResponse> getAllAircraft(String keyword, int page, int size,
                                                         String sortBy, String sortDirection) {
        Pageable pageable = PageableFactory.of(page, size, sortBy, sortDirection,
                SORTABLE_FIELDS, "code", Sort.Direction.ASC);

        String q = keyword == null ? "" : keyword.trim();
        Page<AircraftResponse> result = aircraftRepository.search(q, pageable)
                .map(AircraftMapper::toResponse);

        return PageResponse.from(result);
    }
    @Override
    @Transactional
    public AircraftResponse changeStatusByAdmin(Long id, AircraftStatus status) {
        Aircraft aircraft = aircraftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aircraft", "id", id));

        aircraft.setStatus(status);
        if (status != AircraftStatus.ACTIVE) {
            aircraft.setIsAvailable(false);   // keeps the same rule as the owner validation
        }
        return AircraftMapper.toResponse(aircraftRepository.saveAndFlush(aircraft));
    }

    @Override
    @Transactional
    public void deleteAircraftByAdmin(Long id) {
        Aircraft aircraft = aircraftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aircraft", "id", id));
        aircraftRepository.delete(aircraft);
    }


    private Aircraft findOwnedOrThrow(Long id, Long ownerId) {
        return aircraftRepository.findByIdAndAirlineOwnerId(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Aircraft", "id", id));
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }

    private void validateBusinessRules(AircraftRequest r) {
        List<String> errors = new ArrayList<>();

        // 1. cabin seats must add up to the seating capacity (only if any cabin is specified)
        Integer[] cabins = {r.getEconomySeats(), r.getPremiumEconomySeats(),
                r.getBusinessSeats(), r.getFirstClassSeats()};
        boolean anyCabin = Arrays.stream(cabins).anyMatch(Objects::nonNull);
        if (anyCabin) {
            int sum = Arrays.stream(cabins).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
            if (sum != r.getSeatingCapacity()) {
                errors.add("Cabin seats add up to %d but seating capacity is %d"
                        .formatted(sum, r.getSeatingCapacity()));
            }
        }

        // 2. year of manufacture cannot be in the future
        int currentYear = Year.now().getValue();
        if (r.getYearOfManufacture() != null && r.getYearOfManufacture() > currentYear) {
            errors.add("Year of manufacture cannot be after " + currentYear);
        }

        // 3. registration cannot precede manufacture
        if (r.getRegistrationDate() != null && r.getYearOfManufacture() != null
                && r.getRegistrationDate().getYear() < r.getYearOfManufacture()) {
            errors.add("Registration date cannot be before the year of manufacture");
        }

        // 4. next maintenance cannot precede registration
        if (r.getNextMaintenanceDate() != null && r.getRegistrationDate() != null
                && r.getNextMaintenanceDate().isBefore(r.getRegistrationDate())) {
            errors.add("Next maintenance date cannot be before the registration date");
        }

        // 5. only an ACTIVE aircraft can be marked available
        if (Boolean.TRUE.equals(r.getIsAvailable()) && r.getStatus() != AircraftStatus.ACTIVE) {
            errors.add("An aircraft with status " + r.getStatus() + " cannot be marked as available");
        }

        if (!errors.isEmpty()) {
            throw new BadRequestException(String.join("; ", errors));
        }
    }
}
