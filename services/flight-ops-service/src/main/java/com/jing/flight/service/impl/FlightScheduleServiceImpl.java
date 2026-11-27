package com.jing.flight.service.impl;

import com.jing.flight.client.AirportZones;
import com.jing.flight.service.FlightClock;
import java.time.ZoneId;
import com.jing.common.enumeration.FlightStatus;
import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.ResourceNotFoundException;
import com.jing.common.dto.flight.FlightScheduleRequest;
import com.jing.common.dto.flight.GenerateInstancesRequest;
import com.jing.common.dto.flight.FlightScheduleResponse;
import com.jing.common.dto.flight.GenerateInstancesResponse;
import com.jing.common.pagination.PageResponse;
import com.jing.common.pagination.PageableFactory;
import com.jing.flight.mapper.FlightMapper;
import com.jing.flight.mapper.FlightScheduleMapper;
import com.jing.flight.model.Flight;
import com.jing.flight.model.FlightInstance;
import com.jing.flight.model.FlightSchedule;
import com.jing.flight.repository.FlightInstanceRepository;
import com.jing.flight.repository.FlightRepository;
import com.jing.flight.repository.FlightScheduleRepository;
import com.jing.flight.service.FlightScheduleService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FlightScheduleServiceImpl implements FlightScheduleService {
    private static final int MAX_GENERATE_DAYS = 366;

    private final FlightScheduleRepository scheduleRepository;
    private final FlightRepository flightRepository;
    private final FlightInstanceRepository flightInstanceRepository;
    private final AirportZones airportZones;



    @Override
    @Transactional
    public FlightScheduleResponse createSchedule(FlightScheduleRequest request) {
        if (request == null)  throw new BadRequestException("Request body is required");
        requireId(request.getFlightId(), "Flight ID");

        Flight flight = flightRepository.findById(request.getFlightId())
                .orElseThrow(() -> new ResourceNotFoundException("Flight", "id", request.getFlightId()));

        FlightSchedule schedule = FlightScheduleMapper.toEntity(request, flight);
        validate(schedule);   // validate before saving, not after

        return FlightScheduleMapper.toResponse(scheduleRepository.save(schedule));
    }

    @Override
    public PageResponse<FlightScheduleResponse> getSchedules(Long flightId, Boolean isActive, int page, int size) {
        Pageable pageable = PageableFactory.of(page, size, null, null, Set.of("id"), "id", Sort.Direction.DESC);

        Specification<FlightSchedule> spec = ((root, query, cb) ->{
            List<Predicate> predicates = new ArrayList<>();
            if (flightId != null) predicates.add(cb.equal(root.get("flight").get("id"), flightId));
            if(isActive != null) predicates.add(cb.equal(root.get("isActive"), isActive));
            return cb.and(predicates.toArray(new Predicate[0]));
        });

        return PageResponse.from(
                scheduleRepository.findAll(spec, pageable).map(FlightScheduleMapper::toResponse)
        );
    }

    @Override
    public FlightScheduleResponse getScheduleById(Long id) {
        requireId(id, "Schedule ID");
        return FlightScheduleMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public FlightScheduleResponse updateSchedule(Long id, FlightScheduleRequest request) {
        requireId(id, "Schedule ID");
        if (request == null) throw new BadRequestException("Request body is required");
        FlightSchedule existing = findOrThrow(id);

        if(request.getFlightId() != null && !request.getFlightId().equals(existing.getFlight().getId())) {
            throw new BadRequestException("A schedule cannot be moved to a different flight.");
        }
        FlightScheduleMapper.updateEntity(request, existing);
        validate(existing);
        return FlightScheduleMapper.toResponse(scheduleRepository.save(existing));
    }

    @Override
    @Transactional
    public void deleteSchedule(Long id) {
        requireId(id, "Schedule ID");
        FlightSchedule existing = findOrThrow(id);
        if (flightInstanceRepository.existsByScheduleId(id)) {
            throw new BadRequestException(
                    "This schedule has generated flight instances. Deactivate it instead of deleting it");
        }
        scheduleRepository.delete(existing);
    }

    @Override
    @Transactional
    public GenerateInstancesResponse generateInstances(Long id, GenerateInstancesRequest req) {
        requireId(id, "Schedule ID");
        if (req == null) throw new BadRequestException("Request body is required");
        if (req.getTotalSeats() == null || req.getTotalSeats() <= 0)
            throw new BadRequestException("Total seats must be a positive number");
        validateBookingWindow(req.getMinAdvanceBookingDays(), req.getMaxAdvanceBookingDays());

        FlightSchedule schedule = findOrThrow(id);
        if (!Boolean.TRUE.equals(schedule.getIsActive()))
            throw new BadRequestException("Cannot generate instances from an inactive schedule");

        // Clamp the requested range to the schedule's validity and to today
        LocalDate from = max(req.getFromDate() != null ? req.getFromDate(): schedule.getStartDate(),
                max(schedule.getStartDate(), LocalDate.now()));
        LocalDate to = req.getToDate() != null && req.getToDate().isBefore(schedule.getEndDate())
                ? req.getToDate() : schedule.getEndDate();

        if (to.isBefore(from))
            throw new BadRequestException("No dates to generate: the range is outside the schedule's validity");
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_GENERATE_DAYS)
            throw new BadRequestException("Cannot generate more than " + MAX_GENERATE_DAYS + " days at once");

        Flight flight = schedule.getFlight();
        Set<DayOfWeek> days = schedule.getOperatingDays();
        ZoneId departureZone = airportZones.zoneOf(schedule.getDepartureAirportId());
        ZoneId arrivalZone = airportZones.zoneOf(schedule.getArrivalAirportId());

        Set<LocalDateTime> existing = new HashSet<>(flightInstanceRepository.findDepartureTimes(
                flight.getId(), from.atStartOfDay(), to.plusDays(1).atStartOfDay()));

        List<FlightInstance> toSave = new ArrayList<>();
        int skipped = 0;

        for(LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            if(!days.isEmpty() && !days.contains(d.getDayOfWeek())) continue;

            LocalDateTime departure = d.atTime(schedule.getDepartureTime());
            if(existing.contains(departure)){
                skipped++;
                continue;
            }
            toSave.add(FlightInstance.builder()
                            .flight(flight)
                            .airlineId(flight.getAirlineId())
                            .scheduleId(schedule.getId())
                            .departureAirportId(schedule.getDepartureAirportId())
                            .arrivalAirportId(schedule.getArrivalAirportId())
                            .departureDateTime(departure)
                            .arrivalDateTime(FlightClock.arrivalFor(d, schedule.getDepartureTime(), departureZone,
                                    schedule.getArrivalTime(), arrivalZone))
                            .totalSeats(req.getTotalSeats())
                            .availableSeats(req.getTotalSeats())
                            .status(FlightStatus.SCHEDULED)
                            .minAdvanceBookingDays(req.getMinAdvanceBookingDays())
                            .maxAdvanceBookingDays(req.getMaxAdvanceBookingDays())
                            .isActive(true)
                            .terminal(req.getTerminal())
                            .gate(req.getGate())
                    .build());
        }
        toSave.forEach(fi -> fi.applyTimeZones(departureZone, arrivalZone));
        flightInstanceRepository.saveAll(toSave);
        log.info("Schedule {} generated {} instances ({} skipped) for {} to {}",
                id, toSave.size(), skipped, from, to);
        return GenerateInstancesResponse.builder()
                .scheduleId(id)
                .fromDate(from)
                .toDate(to)
                .created(toSave.size())
                .skipped(skipped)
                .build();
    }

    private void validateBookingWindow(Integer min, Integer max) {
        if (min != null && min < 0) throw new BadRequestException("Minimum advance booking days must not be negative");
        if (max != null && max < 0) throw new BadRequestException("Maximum advance booking days must not be negative");
        if (min != null && max != null && min > max)
            throw new BadRequestException("Minimum advance booking days cannot exceed maximum");
    }



    private void validate(FlightSchedule s){
        requireId(s.getDepartureAirportId(), "Departure airport ID");
        requireId(s.getArrivalAirportId(), "Arrival airport ID");

        if (s.getDepartureAirportId().equals(s.getArrivalAirportId()))
            throw new BadRequestException("Departure and arrival airports must be different");

        if (s.getDepartureTime() == null || s.getArrivalTime() == null)
            throw new BadRequestException("Departure and arrival times are required");
        ZoneId departureZone = airportZones.zoneOf(s.getDepartureAirportId());   // 400 if the airport doesn't exist
        ZoneId arrivalZone = airportZones.zoneOf(s.getArrivalAirportId());
        // the same clock time can be a real flight across time zones (e.g. a 1 h flight one zone west)
        if (departureZone.equals(arrivalZone) && s.getDepartureTime().equals(s.getArrivalTime()))
            throw new BadRequestException("Departure and arrival times must be different");

        if (s.getStartDate() == null || s.getEndDate() == null)
            throw new BadRequestException("Start and end dates are required");
        if (s.getEndDate().isBefore(s.getStartDate()))
            throw new BadRequestException("End date must not be before start date");
        if (s.getOperatingDays().contains(null))
            throw new BadRequestException("Operating days must not contain null values");
    }

    private FlightSchedule findOrThrow(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flight schedule", "id", id));
    }

    private void requireId(Long id, String name) {
        if (id == null || id <= 0)
            throw new BadRequestException(name + " is required and must be a positive number");
    }

    private static LocalDate max(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }
}
