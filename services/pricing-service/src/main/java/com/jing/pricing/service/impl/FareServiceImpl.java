package com.jing.pricing.service.impl;

import com.jing.pricing.client.FareReferences;
import com.jing.common.airline.AirlineAccess;
import com.jing.common.security.CurrentUser;
import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.DuplicateResourceException;
import com.jing.common.exception.ResourceNotFoundException;
import com.jing.common.dto.pricing.FareRequest;
import com.jing.common.dto.pricing.FareResponse;
import com.jing.common.pagination.PageResponse;
import com.jing.common.pagination.PageableFactory;
import com.jing.pricing.mapper.FareMapper;
import com.jing.pricing.model.Fare;
import com.jing.pricing.payload.filter.FareFilter;
import com.jing.pricing.repository.FareRepository;
import com.jing.pricing.service.FareService;
import com.jing.pricing.specification.FareSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FareServiceImpl implements FareService {
    private static final String DEFAULT_SORT_FIELD = "id";
    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "id",
            "fareName",
            "baseFare",
            "currentPrice",
            "createdAt"
    );

    private final FareRepository fareRepository;
    private final AirlineAccess airlineAccess;
    private final FareReferences fareReferences;

    @Override
    @Transactional
    public FareResponse createFare(CurrentUser user, FareRequest fareRequest) {
        // an owner prices only their own airline's flights
        fareRequest.setAirlineId(airlineAccess.airlineForNew(user, fareRequest.getAirlineId()));
        normalizeAndValidate(fareRequest);

        if(fareRepository.existsByFlightIdAndCabinClassIdAndFareName(fareRequest.getFlightId(), fareRequest.getCabinClassId(), fareRequest.getFareName())) {
            throw new DuplicateResourceException("Fare", fareRequest.getFareName(), fareRequest.getFareName());
        }

        fareReferences.requireConsistent(fareRequest.getAirlineId(), fareRequest.getFlightId(),
                fareRequest.getCabinClassId(), fareRequest.getCabinClass());

        Fare saved = fareRepository.save(FareMapper.toEntity(fareRequest));
        log.info("Created fare id={} flightId={} cabinClassId={}", saved.getId(), saved.getFlightId(), saved.getCabinClassId());
        return FareMapper.toResponse(saved);
    }

    @Override
    public FareResponse getById(Long id) {
        return FareMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public FareResponse updateFare(CurrentUser user, Long id, FareRequest request) {
        Fare fare = findOrThrow(id);
        airlineAccess.requireCanManage(user, fare.getAirlineId());
        normalizeAndValidate(request);

        // the fare's identity (airline / flight / cabin class) is fixed after creation
        rejectIfChanged("airlineId", fare.getAirlineId(), request.getAirlineId());
        rejectIfChanged("flightId", fare.getFlightId(), request.getFlightId());
        rejectIfChanged("cabinClassId", fare.getCabinClassId(), request.getCabinClassId());
        rejectIfChanged("cabinClass", fare.getCabinClass(), request.getCabinClass());

        if (!Objects.equals(fare.getFareName(), request.getFareName())
                && fareRepository.existsByFlightIdAndCabinClassIdAndFareNameAndIdNot(
                fare.getFlightId(), fare.getCabinClassId(), request.getFareName(), id)) {
            throw new DuplicateResourceException("Fare", request.getFareName(), request.getFareName());
        }

        // update in place: never replace fareRule / baggagePolicy with new instances
        FareMapper.updateEntity(request,fare);

        return FareMapper.toResponse(fareRepository.save(fare));
    }

    @Override
    @Transactional
    public void deleteFare(CurrentUser user, Long id) {
        Fare fare = findOrThrow(id);
        airlineAccess.requireCanManage(user, fare.getAirlineId());
        // TODO: once booking-service exists, block (or soft-delete) fares that are referenced by bookings
        fareRepository.delete(fare);
        log.info("Deleted fare id={}", id);
    }

    @Override
    public PageResponse<FareResponse> getAll(FareFilter filter, int page, int size, String sortBy, String sortDirection) {
        validateFilter(filter);
        return PageResponse.from(
                fareRepository.findAll(FareSpecification.withFilter(filter), buildPageable(page, size, sortBy, sortDirection))
                        .map(FareMapper::toResponse)
        );
    }


    private void validateFilter(FareFilter filter) {
        if (filter == null) return;

        if (filter.minPrice() != null && filter.minPrice().signum() < 0) {
            throw new BadRequestException("minPrice must not be negative");
        }
        if (filter.maxPrice() != null && filter.maxPrice().signum() < 0) {
            throw new BadRequestException("maxPrice must not be negative");
        }
        if (filter.minPrice() != null && filter.maxPrice() != null
                && filter.minPrice().compareTo(filter.maxPrice()) > 0) {
            throw new BadRequestException("minPrice must be less than or equal to maxPrice");
        }
    }


    private void normalizeAndValidate(FareRequest r) {
        if (r.getFareName() == null || r.getFareName().isBlank()) {
            throw new BadRequestException("fareName is required");
        }
        r.setFareName(r.getFareName().trim());

        if (r.getAirlineId() == null)    throw new BadRequestException("airlineId is required");
        if (r.getFlightId() == null)     throw new BadRequestException("flightId is required");
        if (r.getCabinClassId() == null) throw new BadRequestException("cabinClassId is required");
        if (r.getCabinClass() == null)   throw new BadRequestException("cabinClass is required");

        // currency
        String currency = r.getCurrency() == null || r.getCurrency().isBlank()
                ? "USD" : r.getCurrency().trim().toUpperCase();
        try {
            Currency.getInstance(currency);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid currency code '" + currency + "'");
        }
        r.setCurrency(currency);

        // money
        requireMoney("baseFare", r.getBaseFare(), true);
        requireMoney("taxesAndFees", r.getTaxesAndFees(), false);
        requireMoney("airlineFees", r.getAirlineFees(), false);
        requireMoney("currentPrice", r.getCurrentPrice(), false);
    }

    private void rejectIfChanged(String field, Object current, Object requested) {
        if (requested != null && !Objects.equals(current, requested)) {
            throw new BadRequestException(field + " cannot be changed after the fare is created");
        }
    }


    private void requireMoney(String field, BigDecimal value, boolean required){
        if (value == null) {
            if (required) throw new BadRequestException(field + " is required");
            return;
        }
        if (value.signum() < 0){
            throw new BadRequestException("Value must be positive");
        }
        if (value.stripTrailingZeros().scale() > 2){
            throw new BadRequestException(field + " must have at most 2 decimal places");
        }
        if(value.precision() - value.scale() > 10){
            throw new BadRequestException(field + " is too large.");
        }
    }


    private Fare findOrThrow(Long id){
        return fareRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fare", "id", id));
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDirection) {
        return PageableFactory.of(page, size, sortBy, sortDirection,
                SORTABLE_FIELDS, DEFAULT_SORT_FIELD, Sort.Direction.DESC);
    }

    
}
