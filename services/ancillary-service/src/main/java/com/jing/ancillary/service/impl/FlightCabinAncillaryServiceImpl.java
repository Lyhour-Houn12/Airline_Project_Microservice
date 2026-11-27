package com.jing.ancillary.service.impl;

import com.jing.ancillary.filter.FlightCabinAncillaryFilter;
import com.jing.ancillary.mapper.FlightCabinAncillaryMapper;
import com.jing.ancillary.model.Ancillary;
import com.jing.ancillary.model.FlightCabinAncillary;
import com.jing.ancillary.repository.AncillaryRepository;
import com.jing.ancillary.repository.FlightCabinAncillaryRepository;
import com.jing.ancillary.service.FlightCabinAncillaryService;
import com.jing.ancillary.specification.FlightCabinAncillarySpecification;
import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.DuplicateResourceException;
import com.jing.common.exception.ResourceNotFoundException;
import com.jing.common.dto.ancillary.FlightCabinAncillaryBulkRequest;
import com.jing.common.dto.ancillary.FlightCabinAncillaryItemRequest;
import com.jing.common.dto.ancillary.FlightCabinAncillaryUpdateRequest;
import com.jing.common.dto.ancillary.FlightCabinAncillaryResponse;
import com.jing.common.pagination.PageResponse;
import com.jing.common.pagination.PageableFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FlightCabinAncillaryServiceImpl implements FlightCabinAncillaryService {
    private static final String DEFAULT_SORT_FIELD = "id";
    private static final Set<String> SORTABLE = Set.of(
            "id", "price", "totalQuantity", "soldQuantity", "createdAt", "updatedAt");

    private final FlightCabinAncillaryRepository fcaRepository;
    private final AncillaryRepository ancillaryRepository;


    @Override
    @Transactional
    public List<FlightCabinAncillaryResponse> createFlightCabinAncillaries(FlightCabinAncillaryBulkRequest request) {
        Long cabinId = request.flightInstanceCabinId();

        Set<Long> duplicateAncillaries = new HashSet<>();
        for (FlightCabinAncillaryItemRequest item : request.ancillaries()){
            if (!duplicateAncillaries.add(item.ancillaryId())) {
                throw new BadRequestException(
                        "ancillaryId " + item.ancillaryId() + " appears more than once in the request");
            }
        }

        Map<Long, Ancillary> ancillaries = ancillaryRepository.findAllById(duplicateAncillaries).stream()
                .collect(Collectors.toMap(Ancillary::getId, Function.identity()));

        List<Long> missingAncillary = duplicateAncillaries.stream().filter(id -> !ancillaries.containsKey(id)).sorted().toList();
        if(!missingAncillary.isEmpty()){
            throw new ResourceNotFoundException("Ancillary" , "id", missingAncillary);
        }

        List<Long> inActiveAncillary = ancillaries.values().stream()
                .filter(a -> !Boolean.TRUE.equals(a.getIsActive()))
                .map(Ancillary::getId).sorted().toList();
        if (!inActiveAncillary.isEmpty()) {
            throw new BadRequestException("Ancillary(ies) " + inActiveAncillary + " are inactive and cannot be assigned");
        }

        for (Long ancillaryId: duplicateAncillaries){
            if(fcaRepository.existsByFlightInstanceCabinIdAndAncillaryId(cabinId, ancillaryId)){
                throw new DuplicateResourceException(
                        "Ancillary", "ancillaryId", ancillaryId + " (already assigned to flight instance cabin " + cabinId + ")");
                }

        }

        // TODO: when Feign clients exist, verify the flight instance cabin exists and that
        // its airline matches the airline of every ancillary.

        List<FlightCabinAncillary> entities = new ArrayList<>();
        for (FlightCabinAncillaryItemRequest item : request.ancillaries()) {
            Ancillary ancillary = ancillaries.get(item.ancillaryId());
            boolean isFree = Boolean.TRUE.equals(item.isFree());
            BigDecimal price = resolvePrice(isFree, item.price(), ancillary.getBasePrice());
            entities.add(FlightCabinAncillaryMapper.toEntity(cabinId, item, ancillary, price, isFree));
        }

        List<FlightCabinAncillary> saved = fcaRepository.saveAllAndFlush(entities);
        log.info("Assigned {} ancillary(ies) to flight instance cabin {}", saved.size(), cabinId);
        return saved.stream().map(FlightCabinAncillaryMapper::toResponse).toList();
    }

    @Override
    public FlightCabinAncillaryResponse getById(Long id) {
        return FlightCabinAncillaryMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FlightCabinAncillaryResponse> getAll(FlightCabinAncillaryFilter filter, int page, int size, String sortBy, String sortDirection) {
        return PageResponse.from(
                fcaRepository.findAll(FlightCabinAncillarySpecification.withFilter(filter), buildPageable(page, size, sortBy, sortDirection))
                        .map(FlightCabinAncillaryMapper::toResponse)
        );
    }

    @Override
    @Transactional
    public FlightCabinAncillaryResponse updateFlightCabinAncillary(
            Long id, FlightCabinAncillaryUpdateRequest request) {

        FlightCabinAncillary entity = findOrThrow(id);

        if (request.totalQuantity() != null && request.totalQuantity() < entity.getSoldQuantity()) {
            throw new BadRequestException("totalQuantity cannot be lower than soldQuantity ("
                    + entity.getSoldQuantity() + ")");
        }

        BigDecimal price = resolvePrice(request.isFree(), request.price(), null);
        if (price == null) {
            throw new BadRequestException("price is required when isFree is false");
        }

        FlightCabinAncillaryMapper.updateEntity(entity, request, price);
        return FlightCabinAncillaryMapper.toResponse(fcaRepository.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public FlightCabinAncillaryResponse changeStatus(Long id, boolean active) {
        FlightCabinAncillary entity = findOrThrow(id);
        entity.setIsActive(active);
        return FlightCabinAncillaryMapper.toResponse(fcaRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteFlightCabinAncillary(Long id) {
        FlightCabinAncillary entity = findOrThrow(id);
        if (entity.getSoldQuantity() > 0) {
            throw new BadRequestException("Cannot delete: " + entity.getSoldQuantity()
                    + " unit(s) already sold. Deactivate it instead.");
        }
        fcaRepository.delete(entity);
        log.info("Deleted flight cabin ancillary id={}", id);
    }

    // free -> always 0 (a price above 0 is a contradiction); otherwise the given price or the default
    private BigDecimal resolvePrice(boolean isFree, BigDecimal price, BigDecimal defaultPrice){
        if (isFree){
            if(price != null && price.signum() > 0){
                throw new BadRequestException("Price must be 0 or omitted when free is true");
            }
            return BigDecimal.ZERO.setScale(2);
        }
        return price != null ? price : defaultPrice;
    }


    private FlightCabinAncillary findOrThrow(Long id) {
        return fcaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flight Cabin Ancillary", "id", id));
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDirection) {
        return PageableFactory.of(page, size, sortBy, sortDirection,
                SORTABLE, DEFAULT_SORT_FIELD, Sort.Direction.DESC);
    }
}
