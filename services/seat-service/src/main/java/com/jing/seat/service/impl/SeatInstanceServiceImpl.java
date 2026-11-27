package com.jing.seat.service.impl;

import com.jing.common.enumeration.SeatAvailabilityStatus;
import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.DuplicateResourceException;
import com.jing.common.exception.ResourceNotFoundException;
import com.jing.common.dto.seat.BlockSeatInstanceRequest;
import com.jing.common.dto.seat.DeleteSeatInstancesResponse;
import com.jing.common.dto.seat.GenerateSeatInstanceRequest;
import com.jing.common.dto.seat.UpdateSurchargeRequest;
import com.jing.common.dto.seat.*;
import com.jing.seat.repository.projection.CabinStatusCount;
import com.jing.seat.model.FlightInstanceCabin;
import com.jing.seat.model.Seat;
import com.jing.seat.model.SeatInstance;
import com.jing.seat.repository.FlightInstanceCabinRepository;
import com.jing.seat.repository.SeatInstanceRepository;
import com.jing.seat.repository.SeatRepository;
import com.jing.seat.service.SeatInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SeatInstanceServiceImpl implements SeatInstanceService {
    private final SeatInstanceRepository seatInstanceRepository;
    private final SeatRepository seatRepository;
    private final FlightInstanceCabinRepository flightInstanceCabinRepository;


    @Override
    public GenerateSeatInstanceResponse generate(GenerateSeatInstanceRequest request) {
        Long flightInstanceId = request.flightInstanceId();

        List<FlightInstanceCabin> cabins = flightInstanceCabinRepository.findByFlightInstanceId(flightInstanceId);

        if (cabins.isEmpty()) {
            throw new ResourceNotFoundException(
                    "FlightInstanceCabin", "flightInstanceId", flightInstanceId);
        }

        if (seatInstanceRepository.existsByFlightInstanceId(flightInstanceId)) {
            throw new DuplicateResourceException(
                    "SeatInstances", "flightInstanceId", flightInstanceId);
        }

        List<SeatInstance> seatInstances = new ArrayList<>();
        List<GenerateSeatInstanceResponse.CabinGenerated> perCabin = new ArrayList<>();

        for (FlightInstanceCabin cabin : cabins) {
            List<Seat> seats = seatRepository.findGeneratableSeatsByCabinClassId(cabin.getCabinClass().getId());

            if (seats.isEmpty()) {
                throw new BadRequestException(
                        "Cabin class '%s' has no active seats. Generate seats first."
                                .formatted(cabin.getCabinClass().getName()));
            }

            if (cabin.getTotalSeats() != seats.size()) {
                log.warn("Cabin {} totalSeats={} but {} seats are generatable",
                        cabin.getId(), cabin.getTotalSeats(), seats.size());
            }


            for (Seat seat : seats) {
                seatInstances.add(SeatInstance.builder()
                        .flightInstanceId(flightInstanceId)
                                .flightInstanceCabin(cabin)
                        .seat(seat)
                        .build());
            }
            perCabin.add(new GenerateSeatInstanceResponse.CabinGenerated(cabin.getId(), cabin.getCabinClass().getName(), seats.size()));

        }

        seatInstanceRepository.saveAll(seatInstances);
        log.info("Generated {} seat instances for flight instance {}", seatInstances.size(), flightInstanceId);

        return new GenerateSeatInstanceResponse(flightInstanceId, seatInstances.size(), perCabin);
    }

    @Override
    @Transactional(readOnly = true)
    public FlightSeatMapResponse getSeatMap(Long flightInstanceId) {
        List<SeatInstance> seatInstances = seatInstanceRepository.findAllForSeatMap(flightInstanceId);

        if (seatInstances.isEmpty()) {
            throw new ResourceNotFoundException("SeatInstances", "flightInstanceId", flightInstanceId);
        }

        Map<Long, List<SeatInstance>> byCabin = seatInstances.stream()
                .collect(Collectors.groupingBy(si -> si.getFlightInstanceCabin().getCabinClass().getId(), LinkedHashMap::new, Collectors.toList()));

        List<FlightSeatMapResponse.CabinSeatMap> cabins = byCabin.values().stream()
                .map(cabinsSeats -> {
                    FlightInstanceCabin cabin = cabinsSeats.get(0).getFlightInstanceCabin();

                    Map<Integer, List<SeatInstance>> byRow = cabinsSeats.stream()
                            .collect(Collectors.groupingBy(
                                    si -> si.getSeat().getSeatRow(),
                                    LinkedHashMap::new,
                                    Collectors.toList()));

                    List<FlightSeatMapResponse.SeatRow> rows = byRow.entrySet().stream()
                            .map(e -> new FlightSeatMapResponse.SeatRow(
                                    e.getKey(),
                                    e.getValue().stream()
                                            .map(this::toSeatDto)
                                            .collect(Collectors.toList())
                            ))
                            .toList();

                    int availableSeats = (int) cabinsSeats.stream()
                            .filter(si -> si.getStatus() == SeatAvailabilityStatus.AVAILABLE)
                            .count();

                    return new FlightSeatMapResponse.CabinSeatMap(
                            cabin.getId(),
                            cabin.getCabinClass().getName(),
                            cabinsSeats.size(),
                            availableSeats,
                            rows);
                }).toList();

       return new FlightSeatMapResponse(flightInstanceId, cabins);
    }

    @Override
    @Transactional(readOnly = true)
    public SeatAvailabilitySummary getSummary(Long flightInstanceId) {
        List<FlightInstanceCabin> cabins = flightInstanceCabinRepository.findByFlightInstanceId(flightInstanceId);

        if(cabins.isEmpty()) {
            throw new ResourceNotFoundException("FlightInstanceCabin", "flightInstanceId", flightInstanceId);
        }

        Map<Long, Map<SeatAvailabilityStatus, Long>> countsByCabin = new HashMap<>();

        for (CabinStatusCount c : seatInstanceRepository.countByCabinAndStatus(flightInstanceId)){
            countsByCabin.computeIfAbsent(c.flightInstanceCabinId(), k -> new HashMap<>())
                    .put(c.status(), c.count());
        }

        long total = 0; long available = 0; long held = 0; long booked = 0; long blocked = 0;
        List<SeatAvailabilitySummary.CabinSummary> result = new ArrayList<>();

        for (FlightInstanceCabin cabin : cabins) {
            Map<SeatAvailabilityStatus, Long> counts = countsByCabin.getOrDefault(cabin.getId(), Map.of());

            long availableCount = counts.getOrDefault(SeatAvailabilityStatus.AVAILABLE, 0L);
            long heldCount = counts.getOrDefault(SeatAvailabilityStatus.HELD, 0L);
            long bookedCount = counts.getOrDefault(SeatAvailabilityStatus.BOOKED, 0L);
            long blockedCount = counts.getOrDefault(SeatAvailabilityStatus.BLOCKED, 0L);

            long cabinTotal = availableCount + heldCount + bookedCount + blockedCount;
            result.add(new SeatAvailabilitySummary.CabinSummary(
                    cabin.getId(),
                    cabin.getCabinClass().getName(),
                    new SeatAvailabilitySummary.StatusCounts(
                            cabinTotal, availableCount, heldCount, bookedCount, blockedCount)));
            total += cabinTotal;
            available += availableCount;
            held += heldCount;
            booked += bookedCount;
            blocked += blockedCount;
        }

        return new SeatAvailabilitySummary(
                flightInstanceId,
                new SeatAvailabilitySummary.StatusCounts(total, available, held, booked, blocked),
                result);
    }

    @Override
    @Transactional(readOnly = true)
    public SeatInstanceResponse getById(Long id) {
       return toResponse(findDetailOrThrow(id));
    }

    @Override
    @Transactional
    public SeatInstanceResponse block(Long id, BlockSeatInstanceRequest request) {
        SeatInstance si = findDetailOrThrow(id);
        if (si.getStatus() != SeatAvailabilityStatus.AVAILABLE) {
            throw new BadRequestException(
                    "Only AVAILABLE seats can be blocked; seat %s is %s"
                            .formatted(si.getSeat().getSeatNumber(), si.getStatus()));
        }
        si.setStatus(SeatAvailabilityStatus.BLOCKED);
        si.setBlockedReason(request.reason().trim());
        return toResponse(si);
    }

    @Override
    @Transactional
    public SeatInstanceResponse unblock(Long id) {
        SeatInstance si = findDetailOrThrow(id);
        if (si.getStatus() != SeatAvailabilityStatus.BLOCKED) {
            throw new BadRequestException(
                    "Only BLOCKED seats can be unblocked; seat %s is %s"
                            .formatted(si.getSeat().getSeatNumber(), si.getStatus()));
        }
        si.setStatus(SeatAvailabilityStatus.AVAILABLE);
        si.setBlockedReason(null);
        return toResponse(si);
    }

    @Override
    @Transactional
    public SeatInstanceResponse updateSurcharge(Long id, UpdateSurchargeRequest request) {
        SeatInstance si = findDetailOrThrow(id);
        if (si.getStatus() == SeatAvailabilityStatus.BOOKED) {
            throw new BadRequestException(
                    "Cannot change the surcharge of booked seat %s".formatted(si.getSeat().getSeatNumber()));
        }
        si.setSurcharge(request.surcharge());
        return toResponse(si);
    }


    @Override
    @Transactional
    public DeleteSeatInstancesResponse deleteByFlightInstance(Long flightInstanceId) {
        if (!seatInstanceRepository.existsByFlightInstanceId(flightInstanceId)) {
            throw new ResourceNotFoundException("SeatInstances", "flightInstanceId", flightInstanceId);
        }
        if (seatInstanceRepository.existsByFlightInstanceIdAndStatusIn(
                flightInstanceId, List.of(SeatAvailabilityStatus.BOOKED, SeatAvailabilityStatus.HELD))) {
            throw new BadRequestException(
                    "Flight instance %d has booked or held seats; seat instances cannot be deleted"
                            .formatted(flightInstanceId));
        }
        int deleted = seatInstanceRepository.deleteAllByFlightInstanceId(flightInstanceId);
        log.info("Deleted {} seat instances for flight instance {}", deleted, flightInstanceId);
        return new DeleteSeatInstancesResponse(flightInstanceId, deleted);
    }





    private SeatInstance findDetailOrThrow(Long id) {
        return seatInstanceRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SeatInstance", "id", id));
    }

    private FlightSeatMapResponse.SeatInstanceDto toSeatDto(SeatInstance si) {
        return new FlightSeatMapResponse.SeatInstanceDto(
                si.getId(),
                si.getSeat().getSeatNumber(),
                String.valueOf(si.getSeat().getColumnLetter()),
                si.getSeat().getSeatType(),
                si.getStatus(),
                si.getSurcharge());
    }


    private SeatInstanceResponse toResponse(SeatInstance si) {
        return new SeatInstanceResponse(
                si.getId(),
                si.getFlightInstanceId(),
                si.getFlightInstanceCabin().getId(),
                si.getFlightInstanceCabin().getCabinClass().getName(),
                si.getSeat().getId(),
                si.getSeat().getSeatNumber(),
                si.getSeat().getSeatType(),
                si.getStatus(),
                si.getSurcharge(),
                si.getBlockedReason());
    }
}
