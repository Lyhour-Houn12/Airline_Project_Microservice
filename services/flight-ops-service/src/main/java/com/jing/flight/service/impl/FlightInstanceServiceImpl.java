package com.jing.flight.service.impl;

import com.jing.flight.outbox.Outbox;
import com.jing.flight.service.messaging.FlightCancelledHandler;
import com.jing.common.airline.AirlineAccess;
import com.jing.common.security.CurrentUser;
import com.jing.flight.client.AirportZones;
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
    private final AirportZones airportZones;
    private final AirlineAccess airlineAccess;
    private final Outbox outbox;


    @Override
    @Transactional
    public FlightInstanceResponse createFlightInstanceWithCabins(CurrentUser user, FlightInstanceRequest request) {
        if (request == null) {
            throw new BadRequestException("Request body is required");
        }
        requireId(request.getFlightId(), "Flight ID");

        Flight flight = flightRepository.findById(request.getFlightId())
                .orElseThrow(() -> new ResourceNotFoundException("Flight", "id", request.getFlightId()));

        airlineAccess.requireCanManage(user, flight.getAirlineId());
        if (request.getAirlineId() != null && !request.getAirlineId().equals(flight.getAirlineId())) {
            throw new BadRequestException("A flight instance belongs to its flight's airline (" + flight.getAirlineId() + ")");
        }
        FlightInstance instance = FlightInstanceMapper.toEntity(request, flight);
        instance.setAirlineId(flight.getAirlineId());

        requireId(instance.getAirlineId(), "Airline ID");
        requireId(instance.getScheduleId(), "Schedule ID");
        validateRoute(instance.getDepartureAirportId(), instance.getArrivalAirportId());
        applyTimeZones(instance);
        validateState(instance);

        if (flightInstanceRepository.existsByFlightIdAndDepartureDateTime(
                flight.getId(), instance.getDepartureDateTime())) {
            throw new BadRequestException("A flight instance for this flight already exists at "
                    + instance.getDepartureDateTime());
        }

        FlightInstance saved = flightInstanceRepository.save(instance);
        log.info("Flight instance {} created for flight {} by user {}", saved.getId(), flight.getId(), user.id());
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
    public FlightInstanceResponse updateFlightInstance(CurrentUser user, Long id, FlightInstanceRequest request) {
        requireId(id, "Flight instance ID");
        if (request == null) {
            throw new BadRequestException("Request body is required");
        }
        FlightInstance existing = findOrThrow(id);
        airlineAccess.requireCanManage(user, existing.getAirlineId());
        if (request.getAirlineId() != null && !request.getAirlineId().equals(existing.getAirlineId())) {
            throw new BadRequestException("A flight instance cannot be moved to another airline");
        }
        if (request.getStatus() != null && request.getStatus() != existing.getStatus()) {
            // one door for status changes: it applies the rules and tells booking-service
            throw new BadRequestException("Change the status with PATCH /api/v1/flight-instances/" + id + "/status");
        }
        if (existing.getStatus() == FlightStatus.CANCELLED) {
            throw new BadRequestException("Flight instance " + id + " is CANCELLED and can't be changed");
        }

        if (request.getFlightId() != null && !request.getFlightId().equals(existing.getFlight().getId())) {
            throw new BadRequestException("A flight instance cannot be moved to a different flight");
        }

        FlightInstanceMapper.updateEntity(request, existing);

        // Validate the final merged state, not just the incoming fields
        validateRoute(existing.getDepartureAirportId(), existing.getArrivalAirportId());
        applyTimeZones(existing);
        validateState(existing);

        return toResponse(flightInstanceRepository.save(existing));
    }

    @Override
    @Transactional
    public FlightInstanceResponse changeStatus(CurrentUser user, Long id, FlightStatus status, String reason) {
        requireId(id, "Flight instance ID");
        if (status == null) {
            throw new BadRequestException("Status is required");
        }
        FlightInstance existing = findOrThrow(id);
        airlineAccess.requireCanManage(user, existing.getAirlineId());
        if (existing.getStatus() == status) {
            return toResponse(existing);
        }
        if (existing.getStatus() == FlightStatus.CANCELLED) {
            // passengers have been cancelled and refunded: the flight can't come back
            throw new BadRequestException("Flight instance " + id + " is CANCELLED; create a new flight instance instead");
        }
        existing.setStatus(status);
        if (status == FlightStatus.CANCELLED) {
            // in the same transaction: booking-service cancels and refunds every booking on it
            outbox.enqueue(FlightCancelledHandler.TYPE, existing.getId(), new FlightCancelledHandler.Payload(
                    existing.getId(), existing.getFlight().getFlightNumber(), reason));
            log.info("Flight instance {} ({}) cancelled by user {}: {}", id,
                    existing.getFlight().getFlightNumber(), user.id(), reason);
        }
        return toResponse(flightInstanceRepository.save(existing));
    }


    @Override
    @Transactional
    public void deleteFlightInstance(CurrentUser user, Long id) {
        requireId(id, "Flight instance ID");
        FlightInstance existing = findOrThrow(id);
        airlineAccess.requireCanManage(user, existing.getAirlineId());
        // TODO: once booking-service exists, block deletes when bookings exist (or soft-delete via isActive=false)
        flightInstanceRepository.delete(existing);
    }


    private FlightInstanceResponse toResponse(FlightInstance fi) {
        // Aircraft / airline / airport details come from other services: not wired yet
        return FlightInstanceMapper.toResponse(fi, null, null, null, null);
    }

    /** Looks up both airports (400 if one doesn't exist or has no time zone) and sets the UTC moments. */
    private void applyTimeZones(FlightInstance fi) {
        if (fi.getDepartureDateTime() == null || fi.getArrivalDateTime() == null) {
            throw new BadRequestException("Departure and arrival date-times are required");
        }
        fi.applyTimeZones(airportZones.zoneOf(fi.getDepartureAirportId()), airportZones.zoneOf(fi.getArrivalAirportId()));
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
        // compare on the UTC clock: 10:00 Tokyo -> 03:00 Los Angeles the same day is a valid flight
        if (!fi.getArrivalAt().isAfter(fi.getDepartureAt())) {
            throw new BadRequestException("Arrival must be after departure (local times "
                    + fi.getDepartureDateTime() + " " + fi.getDepartureTimeZone() + " -> "
                    + fi.getArrivalDateTime() + " " + fi.getArrivalTimeZone() + ")");
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