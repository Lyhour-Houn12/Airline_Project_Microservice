package com.jing.seat.service.impl;


import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.DuplicateResourceException;
import com.jing.common.exception.ResourceNotFoundException;
import com.jing.common.dto.seat.SeatMapRequest;
import com.jing.common.pagination.PageResponse;
import com.jing.common.pagination.PageableFactory;
import com.jing.common.dto.seat.SeatMapResponse;
import com.jing.seat.mapper.SeatMapMapper;
import com.jing.seat.model.CabinClass;
import com.jing.seat.model.SeatMap;
import com.jing.seat.repository.CabinClassRepository;
import com.jing.seat.repository.SeatMapRepository;
import com.jing.seat.service.SeatMapService;
import com.jing.seat.specification.SeatMapSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeatMapServiceImpl implements SeatMapService {
    private static final String DEFAULT_SORT_FIELD = "id";
    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "name", "totalRows", "createdAt");


    private final SeatMapRepository seatMapRepository;
    private final CabinClassRepository cabinClassRepository;

    @Override
    @Transactional
    public SeatMapResponse createSeatMap(SeatMapRequest request) {
        CabinClass cabinClass = cabinClassRepository.findById(request.getCabinClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Cabin Class", "id", request.getCabinClassId()));

        validateDuplicate(request.getName().trim(), request.getAirlineId(), cabinClass.getId(), null);

        if (seatMapRepository.existsByCabinClassId(request.getCabinClassId())) {
            throw new DuplicateResourceException("Cabin class " , "id", request.getCabinClassId() + " already has a seat map");
        }

        SeatMap seatMap = SeatMapMapper.toEntity(request, cabinClass);
        seatMap.setName(request.getName().trim());

        SeatMap savedSeatMap = seatMapRepository.save(seatMap);

        return SeatMapMapper.toResponse(savedSeatMap);
    }

    @Override
    public SeatMapResponse getSeatMapById(Long id) {
        SeatMap seatMap = seatMapRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SeatMap", "id", id));
        return SeatMapMapper.toResponse(seatMap);
    }

    @Override
    public SeatMapResponse getSeatMapByCabin(Long cabinId) {
        SeatMap seatMap = seatMapRepository.findFirstByCabinClass_IdOrderByIdAsc(cabinId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat map", "cabinClassId", cabinId));

        return SeatMapMapper.toResponse(seatMap);
    }

    @Override
    public PageResponse<SeatMapResponse> getSeatMaps(String keyword, Long cabinId, Long airlineId, int page, int size, String sortBy, String sortDirection) {
        Page<SeatMap> seatMapPage = seatMapRepository.findAll(SeatMapSpecification.filter(keyword, cabinId, airlineId),
                buildPageable(page, size, sortBy, sortDirection)
        );

        Page<SeatMapResponse> responsePage = seatMapPage.map(
                SeatMapMapper::toResponse
        );

        return PageResponse.from(responsePage);
    }

    @Override
    @Transactional
    public SeatMapResponse updateSeatMap(Long id, SeatMapRequest request) {

        SeatMap seatMap = seatMapRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seat Map", "id", id));

        CabinClass cabinClass = cabinClassRepository
                .findById(request.getCabinClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Cabin Class", "id", request.getCabinClassId()));

        validateDuplicate(request.getName().trim(), request.getAirlineId(), cabinClass.getId(), id);   // exclude itself, or its own name counts as a duplicate

        if(seatMapRepository.existsByCabinClassIdAndIdNot(request.getCabinClassId(), id)){
            throw new DuplicateResourceException("Cabin class " , "id", request.getCabinClassId() + " already has a seat map");

        }

        SeatMapMapper.updateEntity(seatMap, request, cabinClass);
        SeatMap updatedSeatMap = seatMapRepository.save(seatMap);
        return SeatMapMapper.toResponse(updatedSeatMap);
    }

    @Override
    @Transactional
    public void deleteSeatMap(Long id) {

        SeatMap seatMap = seatMapRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seat Map", "id", id));
        seatMapRepository.delete(seatMap);
    }

    private void validateDuplicate(
            String name,
            Long airlineId,
            Long cabinClassId,
            Long excludeSeatMapId
    ) {
        boolean duplicateExists;

        if (excludeSeatMapId == null) {
            duplicateExists =
                    seatMapRepository.existsByNameIgnoreCaseAndAirlineIdAndCabinClass_Id(
                            name,
                            airlineId,
                            cabinClassId
                    );
        } else {
            duplicateExists =
                    seatMapRepository
                            .existsByNameIgnoreCaseAndAirlineIdAndCabinClass_IdAndIdNot(
                                    name,
                                    airlineId,
                                    cabinClassId,
                                    excludeSeatMapId
                            );
        }

        if (duplicateExists) {
            throw new DuplicateResourceException(
                    "Seat map",
                    "name",
                    name
            );
        }
    }


    private Pageable buildPageable(int page, int size, String sortBy, String sortDirection) {
        return PageableFactory.of(page, size, sortBy, sortDirection,
                SORTABLE_FIELDS, DEFAULT_SORT_FIELD, Sort.Direction.ASC);
    }
}