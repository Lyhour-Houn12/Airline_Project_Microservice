package com.jing.flight.service.impl;

import com.jing.flight.client.AirportZones;
import com.jing.common.enumeration.FlightStatus;
import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.DuplicateResourceException;
import com.jing.common.exception.ResourceNotFoundException;
import com.jing.common.dto.flight.FlightRequest;
import com.jing.common.dto.flight.FlightResponse;
import com.jing.common.pagination.PageResponse;
import com.jing.common.pagination.PageableFactory;
import com.jing.flight.mapper.FlightMapper;
import com.jing.flight.model.Flight;
import com.jing.flight.repository.FlightRepository;
import com.jing.flight.service.FlightService;
import com.jing.flight.specification.FlightSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;


@Service
@RequiredArgsConstructor
public class FlightServiceImpl implements FlightService {

    private static final int MAX_BATCH_SIZE = 100;
    private static final int MAX_IDS_LOOKUP = 200;
    private static final String DEFAULT_SORT_FIELD = "createdAt";
    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "flightNumber", "status", "createdAt");


    private static final Pattern FLIGHT_NUMBER_PATTERN = Pattern.compile("^[A-Z0-9]{2}[0-9]{1,4}$");
    private final FlightRepository flightRepository;
    private final AirportZones airportZones;


    @Override
    @Transactional
    public FlightResponse createFlight(Long userId, FlightRequest request) {
        requireId(userId, "User ID");
        requireNotNull(request, "Flight request is required");

        String flightNumber = normalizeFlightNumber(request.getFlightNumber());
        if (flightRepository.existsByFlightNumber(flightNumber)) {
            throw new DuplicateResourceException("Flight", "flight number", request.getFlightNumber());
        }

        Flight flight = buildNewFlight(request, flightNumber);
        return toResponse(flightRepository.save(flight));
    }


    @Override
    @Transactional
    public List<FlightResponse> createFlights(Long userId, List<FlightRequest> requests) {
        requireId(userId, "User ID");
        if (requests == null || requests.isEmpty()) {
            throw new BadRequestException("At least one flight is required");
        }
        if (requests.size() > MAX_BATCH_SIZE) {
            throw new BadRequestException("You can create at most " + MAX_BATCH_SIZE + " flights at once");
        }

        Set<String> seenNumbers = new HashSet<>();
        List<Flight> flights = new ArrayList<>(requests.size());

        for (int i = 0; i < requests.size(); i++) {
            FlightRequest request = requests.get(i);
            if (request == null) {
                throw new BadRequestException("Flight request at index " + i + " is empty");
            }
            String flightNumber = normalizeFlightNumber(request.getFlightNumber());
            if (!seenNumbers.add(flightNumber)) {
                throw new BadRequestException("Duplicate flight number in request: " + flightNumber);
            }
            flights.add(buildNewFlight(request, flightNumber));
        }

        List<String> existing = flightRepository.findExistingFlightNumbers(seenNumbers);
        if (!existing.isEmpty()) {
            throw new DuplicateResourceException("Flight", "flight number(s)", existing);
        }

        List<FlightResponse> responses = new ArrayList<>(flights.size());
        for (Flight saved : flightRepository.saveAll(flights)) {
            responses.add(toResponse(saved));
        }
        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public FlightResponse getFlightById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public FlightResponse getFlightByNumber(String flightNumber) {
        String normalized = normalizeFlightNumber(flightNumber);
        Flight flight = flightRepository.findByFlightNumber(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Flight", "flight number", normalized));
        return toResponse(flight);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FlightResponse> getFlightsByAirline(Long userId, String keyword, Long airlineId, Long aircraftId, Long departureAirportId, Long arrivalAirportId, FlightStatus status, int page, int size, String sortBy, String sortDirection) {
        Specification<Flight> spec = FlightSpecification.withFilters(keyword, airlineId, aircraftId, departureAirportId, arrivalAirportId, status);

        Page<Flight> result = flightRepository.findAll(spec, buildPageable(page, size, sortBy, sortDirection));
        return PageResponse.from(result.map(this::toResponse));
    }


    @Override
    @Transactional
    public FlightResponse updateFlight(Long id, FlightRequest request) {
        requireNotNull(request, "Flight request is required");
        Flight existing = findOrThrow(id);

        if (request.getAirlineId() != null && !request.getAirlineId().equals(existing.getAirlineId())) {
            throw new BadRequestException("A flight cannot be moved to another airline");
        }

        if (request.getFlightNumber() != null) {
            String flightNumber = normalizeFlightNumber(request.getFlightNumber());
            if (!flightNumber.equals(existing.getFlightNumber())
                    && flightRepository.existsByFlightNumberAndIdNot(flightNumber, id)) {
                throw new DuplicateResourceException("Flight", "flight number", flightNumber);
            }
            request.setFlightNumber(flightNumber);
        }

        if (request.getAircraftId() != null) requireId(request.getAircraftId(), "Aircraft ID");
        if (request.getDepartureAirportId() != null) requireId(request.getDepartureAirportId(), "Departure airport ID");
        if (request.getArrivalAirportId() != null) requireId(request.getArrivalAirportId(), "Arrival airport ID");

        Long departureId = request.getDepartureAirportId() != null
                ? request.getDepartureAirportId() : existing.getDepartureAirportId();
        Long arrivalId = request.getArrivalAirportId() != null
                ? request.getArrivalAirportId() : existing.getArrivalAirportId();
        validateRoute(departureId, arrivalId);

        FlightMapper.updateEntity(request, existing);
        return toResponse(flightRepository.save(existing));
    }

    @Override
    @Transactional
    public FlightResponse changeStatus(Long id, FlightStatus status) {
        requireNotNull(status, "Flight status is required");
        Flight flight = findOrThrow(id);

        if (flight.getStatus() != status) {
            flight.setStatus(status);
            flight = flightRepository.save(flight);
        }
        return toResponse(flight);
    }

    @Override
    @Transactional
    public void deleteFlight(Long id) {
        flightRepository.delete(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, FlightResponse> getFlightsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        List<Long> distinctIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctIds.size() > MAX_IDS_LOOKUP) {
            throw new BadRequestException("You can look up at most " + MAX_IDS_LOOKUP + " flights at once");
        }

        Map<Long, FlightResponse> result = new LinkedHashMap<>();
        for (Flight flight : flightRepository.findAllById(distinctIds)) {
            result.put(flight.getId(), toResponse(flight));
        }
        return result;
    }



    private Flight buildNewFlight(FlightRequest request, String flightNumber) {
        // no airline lookup yet, so the airline id must come from the request
        requireId(request.getAirlineId(), "Airline ID");
        requireId(request.getAircraftId(), "Aircraft ID");
        validateRoute(request.getDepartureAirportId(), request.getArrivalAirportId());

        Flight flight = FlightMapper.toEntity(request);
        flight.setFlightNumber(flightNumber);
        flight.setAirlineId(request.getAirlineId());
        return flight;
    }


    private void validateRoute(Long departureAirportId, Long arrivalAirportId) {
        requireId(departureAirportId, "Departure airport ID");
        requireId(arrivalAirportId, "Arrival airport ID");
        if (departureAirportId.equals(arrivalAirportId)) {
            throw new BadRequestException("Departure and arrival airports must be different");
        }
        // both airports must really exist in location-service (and have a time zone, for the instances)
        airportZones.zoneOf(departureAirportId);
        airportZones.zoneOf(arrivalAirportId);
    }

    private String normalizeFlightNumber(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("Flight number is required");
        }
        String normalized = raw.replaceAll("\\s+", "").toUpperCase();
        if (!FLIGHT_NUMBER_PATTERN.matcher(normalized).matches()) {
            throw new BadRequestException(
                    "Invalid flight number '" + raw + "'. Expected airline code + 1-4 digits, e.g. VN123");
        }
        return normalized;
    }

    private Flight findOrThrow(Long id) {
        requireId(id, "Flight ID");
        return flightRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flight" , "id" , id));
    }

    // TODO: pass aircraft / airline / airports here once the clients exist
    private FlightResponse toResponse(Flight flight) {
        return FlightMapper.toResponse(flight, null, null, null, null);
    }

    private void requireNotNull(Object value, String message) {
        if (value == null) {
            throw new BadRequestException(message);
        }
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
