package com.jing.seat.service.impl;

import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.ResourceNotFoundException;
import com.jing.common.dto.seat.FlightInstanceCabinBulkRequest;
import com.jing.seat.filter.FlightInstanceCabinFilter;
import com.jing.common.dto.seat.FlightInstanceCabinRequest;
import com.jing.common.dto.seat.FlightInstanceCabinUpdateRequest;
import com.jing.common.dto.seat.FlightInstanceCabinResponse;
import com.jing.common.pagination.PageResponse;
import com.jing.common.pagination.PageableFactory;
import com.jing.seat.mapper.FlightInstanceCabinMapper;
import com.jing.seat.model.CabinClass;
import com.jing.seat.model.FlightInstanceCabin;
import com.jing.seat.repository.CabinClassRepository;
import com.jing.seat.repository.FlightInstanceCabinRepository;
import com.jing.seat.repository.SeatRepository;
import com.jing.seat.service.FlightInstanceCabinService;
import com.jing.seat.specification.FlightInstanceCabinSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class FlightInstanceCabinServiceImpl implements FlightInstanceCabinService {

    private static final String DEFAULT_SORT_FIELD = "id";
    private static final Set<String> SORTABLE_FIELDS =Set.of("id", "flightInstanceId", "totalSeats", "bookedSeats", "isBookable", "createdAt");
    private final FlightInstanceCabinRepository cabinRepository;
    private final SeatRepository seatRepository;
    private final CabinClassRepository cabinClassRepository;

    @Override
    public List<FlightInstanceCabinResponse> createCabins(FlightInstanceCabinBulkRequest request) {
        Long flightInstanceId = request.flightInstanceId();

        Set<Long> requestedIds = new HashSet<>();
        for (FlightInstanceCabinRequest item : request.cabins()){
            if (!requestedIds.add(item.cabinClassId())){
                throw new BadRequestException("Cabin class " + item.cabinClassId() + " appears twice in the request");
            }
        }

        List<FlightInstanceCabin> existing = cabinRepository.findByFlightInstanceIdOrderByCabinClassDisplayOrderAsc(flightInstanceId);

        Set<Long> existingCabinIds = new HashSet<>();
        existing.forEach(c -> existingCabinIds.add(c.getCabinClass().getId()));

        // all cabins of one flight instance must belong to the same aircraft
        Long expectedAircraftId = existing.isEmpty() ? null : existing.get(0).getCabinClass().getAircraftId();

        List<FlightInstanceCabin> toSave = new ArrayList<>();

        for (FlightInstanceCabinRequest item : request.cabins()){
            CabinClass cabin = cabinClassRepository.findById(item.cabinClassId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cabin Class", "id", item.cabinClassId()));

            if (!Boolean.TRUE.equals(cabin.getIsActive()) || !Boolean.TRUE.equals(cabin.getIsBookable())) {
                throw new BadRequestException("Cabin class " + cabin.getId() + " is not active and bookable");
            }
            if (existingCabinIds.contains(cabin.getId())) {
                throw new BadRequestException("Flight instance " + flightInstanceId
                        + " already has cabin class " + cabin.getId());
            }
            if (expectedAircraftId == null) {
                expectedAircraftId = cabin.getAircraftId();
            } else if (!expectedAircraftId.equals(cabin.getAircraftId())) {
                throw new BadRequestException("Cabin class " + cabin.getId()
                        + " belongs to a different aircraft than the other cabins of this flight instance");
            }

            long usable = seatRepository.countUsableByCabinClassId(cabin.getId());
            if (usable == 0) {
                throw new BadRequestException("Cabin class " + cabin.getId()
                        + " has no usable seats; generate seats first");
            }
            int totalSeats = item.totalSeats() != null ? item.totalSeats() : (int) usable;
            if (totalSeats > usable) {
                throw new BadRequestException("totalSeats " + totalSeats + " exceeds the " + usable
                        + " usable seats of cabin class " + cabin.getId());
            }
            toSave.add(FlightInstanceCabinMapper.toEntity(flightInstanceId, cabin, totalSeats, item.isBookable()));
        }
        try {
            return cabinRepository.saveAllAndFlush(toSave).stream()
                    .map(FlightInstanceCabinMapper::toResponse).toList();
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("One of these cabins already exists on flight instance " + flightInstanceId);
        }
    }




    @Override
    @Transactional(readOnly = true)
    public FlightInstanceCabinResponse getById(Long id) {
        return FlightInstanceCabinMapper.toResponse(get(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlightInstanceCabinResponse> getByFlightInstanceId(Long flightInstanceId) {
        return cabinRepository.findByFlightInstanceIdOrderByCabinClassDisplayOrderAsc(flightInstanceId)
                .stream()
                .map(FlightInstanceCabinMapper::toResponse).toList();
    }

    @Override
    public PageResponse<FlightInstanceCabinResponse> search(FlightInstanceCabinFilter filter, int page, int size, String sortBy, String sortDirection) {
        return PageResponse.from(
                cabinRepository.findAll(FlightInstanceCabinSpecification.toSpec(filter), buildPageable(page, size, sortBy, sortDirection))
                        .map(FlightInstanceCabinMapper::toResponse)

        );
    }

    @Override
    public FlightInstanceCabinResponse update(Long id, FlightInstanceCabinUpdateRequest request) {
        FlightInstanceCabin cabin = get(id);

        if (request.totalSeats() != null){
            long usable = seatRepository.countUsableByCabinClassId(cabin.getCabinClass().getId());
            if (request.totalSeats() > usable){
                throw new BadRequestException("totalSeats " + request.totalSeats() + " exceeds the "
                        + usable + " usable seats of this cabin class");
            }
            if (request.totalSeats() < cabin.getBookedSeats()) {
                throw new BadRequestException("totalSeats cannot be lower than the " + cabin.getBookedSeats()
                        + " seats already booked");
            }
            cabin.setTotalSeats(request.totalSeats());

        }
        if (request.isBookable() != null) {
            cabin.setIsBookable(request.isBookable());
        }
        try {
            return FlightInstanceCabinMapper.toResponse(cabinRepository.saveAndFlush(cabin));
        } catch (OptimisticLockingFailureException e) {
            throw new BadRequestException("This cabin was modified by someone else; reload and try again");
        }

    }
    @Override
    public void delete(Long id) {
        FlightInstanceCabin cabin = get(id);
        if (cabin.getBookedSeats() > 0){
            throw new BadRequestException("Cannot delete a cabin with " + cabin.getBookedSeats() + " booked seats");
        }
        cabinRepository.delete(cabin);

    }

    private FlightInstanceCabin get(Long id) {
        return cabinRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flight Instance Cabin", "id", id));
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDirection) {
        return PageableFactory.of(page, size, sortBy, sortDirection,
                SORTABLE_FIELDS, DEFAULT_SORT_FIELD, Sort.Direction.ASC);
    }

}
