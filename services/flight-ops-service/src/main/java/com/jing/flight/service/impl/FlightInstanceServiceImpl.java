package com.jing.flight.service.impl;

import com.jing.common.enumeration.FlightStatus;
import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.ResourceNotFoundException;
import com.jing.common.dto.flight.FlightInstanceRequest;
import com.jing.common.dto.flight.FlightInstanceResponse;
import com.jing.common.pagination.PageResponse;
import com.jing.common.pagination.PageableFactory;
import com.jing.flight.filter.FlightInstanceFilter;
import com.jing.flight.mapper.FlightInstanceMapper;
import com.jing.flight.model.Flight;
import com.jing.flight.model.FlightInstance;
import com.jing.flight.repository.FlightInstanceRepository;
import com.jing.flight.repository.FlightRepository;
import com.jing.flight.service.FlightInstanceService;
import com.jing.flight.specification.FlightInstanceSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FlightInstanceServiceImpl implements FlightInstanceService {
    private static final int MAX_BATCH_SIZE = 100;
    private static final int MAX_IDS_LOOKUP = 200;
    private static final String DEFAULT_SORT_FIELD = "createdAt";
    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "availableSeats", "totalSeats", "createdAt");

    private final FlightInstanceRepository flightInstanceRepository;
    private final FlightRepository flightRepository;


    @Override
    @Transactional
    public FlightInstanceResponse createFlightInstanceWithCabins(Long userId, FlightInstanceRequest request) {
        if (request == null) {
            throw new BadRequestException("Request body is required");
        }
        requireId(request.getFlightId(), "Flight ID");

        Flight flight = flightRepository.findById(request.getFlightId())
                .orElseThrow(() -> new ResourceNotFoundException("Flight", "id", request.getFlightId()));

        FlightInstance instance = FlightInstanceMapper.toEntity(request, flight);

        requireId(instance.getAirlineId(), "Airline ID");
        requireId(instance.getScheduleId(), "Schedule ID");
        validateRoute(instance.getDepartureAirportId(), instance.getArrivalAirportId());
        validateState(instance);

        if (flightInstanceRepository.existsByFlightIdAndDepartureDateTime(
                flight.getId(), instance.getDepartureDateTime())) {
            throw new BadRequestException("A flight instance for this flight already exists at "
                    + instance.getDepartureDateTime());
        }

        // TODO: create cabins once the cabin model exists. userId is reserved for audit/ownership checks.
        FlightInstance saved = flightInstanceRepository.save(instance);
        log.info("Flight instance {} created for flight {} by user {}", saved.getId(), flight.getId(), userId);
        return toResponse(saved);
    }


    @Override
    public PageResponse<FlightInstanceResponse> getFlightInstances(FlightInstanceFilter filter,
                                                                   boolean publicView,
                                                                   int page, int size,
                                                                   String sortBy, String sortDirection) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDirection);

        Page<FlightInstance> result = flightInstanceRepository
                .findAll(FlightInstanceSpecification.build(filter, publicView), pageable);

        return PageResponse.from(result.map(this::toResponse));
    }

    @Override
    public FlightInstanceResponse getFlightInstanceById(Long id) {
        requireId(id, "Flight instance ID");
        return toResponse(findOrThrow(id));
    }

    @Override
    public Map<Long, FlightInstanceResponse> getFlightInstancesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        if (ids.size() > MAX_IDS_LOOKUP) {
            throw new BadRequestException("Cannot look up more than " + MAX_IDS_LOOKUP + " flight instances at once");
        }
        Set<Long> distinct = new LinkedHashSet<>();
        for (Long id : ids) {
            requireId(id, "Flight instance ID");
            distinct.add(id);
        }
        return flightInstanceRepository.findAllById(distinct).stream()
                .collect(Collectors.toMap(FlightInstance::getId, this::toResponse,
                        (a, b) -> a, LinkedHashMap::new));
    }


    @Override
    @Transactional
    public FlightInstanceResponse updateFlightInstance(Long id, FlightInstanceRequest request) {
        requireId(id, "Flight instance ID");
        if (request == null) {
            throw new BadRequestException("Request body is required");
        }
        FlightInstance existing = findOrThrow(id);

        if (request.getFlightId() != null && !request.getFlightId().equals(existing.getFlight().getId())) {
            throw new BadRequestException("A flight instance cannot be moved to a different flight");
        }

        FlightInstanceMapper.updateEntity(request, existing);

        // Validate the final merged state, not just the incoming fields
        validateRoute(existing.getDepartureAirportId(), existing.getArrivalAirportId());
        validateState(existing);

        return toResponse(flightInstanceRepository.save(existing));
    }

    @Override
    @Transactional
    public FlightInstanceResponse changeStatus(Long id, FlightStatus status) {
        requireId(id, "Flight instance ID");
        if (status == null) {
            throw new BadRequestException("Status is required");
        }
        FlightInstance existing = findOrThrow(id);
        if (existing.getStatus() == status) {
            return toResponse(existing);
        }
        // TODO: add transition rules once you decide them (e.g. no changes out of CANCELLED)
        existing.setStatus(status);
        return toResponse(flightInstanceRepository.save(existing));
    }


    @Override
    @Transactional
    public void deleteFlightInstance(Long id) {
        requireId(id, "Flight instance ID");
        FlightInstance existing = findOrThrow(id);
        // TODO: once booking-service exists, block deletes when bookings exist (or soft-delete via isActive=false)
        flightInstanceRepository.delete(existing);
    }


    private FlightInstanceResponse toResponse(FlightInstance fi) {
        // Aircraft / airline / airport details come from other services: not wired yet
        return FlightInstanceMapper.toResponse(fi, null, null, null, null);
    }

    private void validateRoute(Long departureAirportId, Long arrivalAirportId) {
        requireId(departureAirportId, "Departure airport ID");
        requireId(arrivalAirportId, "Arrival airport ID");
        if (departureAirportId.equals(arrivalAirportId)) {
            throw new BadRequestException("Departure and arrival airports must be different");
        }
    }

    private void validateState(FlightInstance fi) {
        if (fi.getDepartureDateTime() == null) {
            throw new BadRequestException("Departure date-time is required");
        }
        if (fi.getArrivalDateTime() == null) {
            throw new BadRequestException("Arrival date-time is required");
        }
        if (!fi.getArrivalDateTime().isAfter(fi.getDepartureDateTime())) {
            throw new BadRequestException("Arrival date-time must be after departure date-time");
        }

        Integer total = fi.getTotalSeats();
        Integer available = fi.getAvailableSeats();
        if (total == null || total <= 0) {
            throw new BadRequestException("Total seats must be a positive number");
        }
        if (available == null || available < 0) {
            throw new BadRequestException("Available seats must not be negative");
        }
        if (available > total) {
            throw new BadRequestException("Available seats cannot exceed total seats");
        }

        Integer min = fi.getMinAdvanceBookingDays();
        Integer max = fi.getMaxAdvanceBookingDays();
        if (min != null && min < 0) {
            throw new BadRequestException("Minimum advance booking days must not be negative");
        }
        if (max != null && max < 0) {
            throw new BadRequestException("Maximum advance booking days must not be negative");
        }
        if (min != null && max != null && min > max) {
            throw new BadRequestException("Minimum advance booking days cannot exceed maximum");
        }

        validateText(fi.getTerminal(), "Terminal", 10);
        validateText(fi.getGate(), "Gate", 10);
    }

    private void validateText(String value, String name, int maxLength) {
        if (value != null && value.length() > maxLength) {
            throw new BadRequestException(name + " must be at most " + maxLength + " characters");
        }
    }

    private FlightInstance findOrThrow(Long id) {
        return flightInstanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flight instance", "id", id));
    }

    private void requireId(Long id, String name) {
        if (id == null || id <= 0) {
            throw new BadRequestException(name + " is required and must be a positive number");
        }
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDirection) {
        return PageableFactory.of(page, size, sortBy, sortDirection,
                SORTABLE_FIELDS, DEFAULT_SORT_FIELD, Sort.Direction.DESC);
    }
}