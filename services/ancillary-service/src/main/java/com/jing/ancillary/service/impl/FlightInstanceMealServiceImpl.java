package com.jing.ancillary.service.impl;

import com.jing.ancillary.filter.FlightInstanceMealFilter;
import com.jing.ancillary.mapper.FlightInstanceMealMapper;
import com.jing.ancillary.model.FlightInstanceMeal;
import com.jing.ancillary.model.Meal;
import com.jing.ancillary.repository.FlightInstanceMealRepository;
import com.jing.ancillary.repository.MealRepository;
import com.jing.ancillary.service.FlightInstanceMealService;
import com.jing.ancillary.specification.FlightInstanceMealSpecification;
import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.ResourceNotFoundException;
import com.jing.common.dto.ancillary.FlightInstanceMealBulkRequest;
import com.jing.common.dto.ancillary.FlightInstanceMealItemRequest;
import com.jing.common.dto.ancillary.FlightInstanceMealUpdateRequest;
import com.jing.common.dto.ancillary.FlightInstanceMealResponse;
import com.jing.common.pagination.PageResponse;
import com.jing.common.pagination.PageableFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FlightInstanceMealServiceImpl implements FlightInstanceMealService {
    private static final String DEFAULT_SORT_FIELD = "id";
    private static final Set<String> SORTABLE = Set.of(
            "id", "price", "totalQuantity", "soldQuantity", "orderCutoffHours",
            "createdAt", "updatedAt");

    private final FlightInstanceMealRepository fimRepository;
    private final MealRepository mealRepository;


    @Override
    @Transactional
    public List<FlightInstanceMealResponse> createFlightInstanceMeals(FlightInstanceMealBulkRequest request) {
        Long flightInstanceId = request.flightInstanceId();

        // 1. no duplicate meals inside the request
        Set<Long> duplicateMeal = new HashSet<>();
        for (FlightInstanceMealItemRequest item : request.meals()) {
            if (!duplicateMeal.add(item.mealId())) {
                throw new BadRequestException("mealId " + item.mealId() + " appears more than once in the request");
            }
        }

        Map<Long, Meal> meals = mealRepository.findAllById(duplicateMeal).stream()
                .collect(Collectors.toMap(Meal::getId, Function.identity()));

        Long missingMealId = duplicateMeal.stream()
                .filter(id -> !meals.containsKey(id))
                .findFirst()
                .orElse(null);
        if (missingMealId != null) {
            throw new ResourceNotFoundException("Meal", "id", missingMealId.toString());
        }

        List<Long> inActiveMeal = meals.values().stream()
                .filter(meal -> !meal.getIsActive())
                .map(Meal::getId)
                .toList();
        if (!inActiveMeal.isEmpty()) {
            throw new BadRequestException("Meal(s) " + inActiveMeal + " are inactive and cannot be assigned");
        }

        for(Long mealId : duplicateMeal) {
            if (fimRepository.existsByFlightInstanceIdAndMealId(flightInstanceId, mealId)) {
                throw new BadRequestException("mealId " + mealId + " is already assigned to flightInstanceId " + flightInstanceId);
            }
        }

        List<FlightInstanceMeal> entities = request.meals().stream()
                .map(item -> FlightInstanceMealMapper.toEntity(
                        flightInstanceId, item, meals.get(item.mealId())))
                .toList();

        List<FlightInstanceMeal> saved = fimRepository.saveAllAndFlush(entities);
        log.info("Assigned {} meal(s) to flight instance {}", saved.size(), flightInstanceId);
        return saved.stream().map(FlightInstanceMealMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FlightInstanceMealResponse getById(Long id) {
        return FlightInstanceMealMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FlightInstanceMealResponse> getAll(FlightInstanceMealFilter filter, int page, int size, String sortBy, String sortDirection) {
        return PageResponse.from(
                fimRepository.findAll(FlightInstanceMealSpecification.withFilter(filter), buildPageable(page, size, sortBy, sortDirection))
                        .map(FlightInstanceMealMapper::toResponse)
        );
    }

    @Override
    @Transactional
    public FlightInstanceMealResponse updateFlightInstanceMeal(Long id, FlightInstanceMealUpdateRequest request) {
        FlightInstanceMeal entity = findOrThrow(id);

        if (request.totalQuantity() < entity.getSoldQuantity()) {
            throw new BadRequestException("totalQuantity cannot be lower than soldQuantity ("
                    + entity.getSoldQuantity() + ")");
        }

        FlightInstanceMealMapper.updateEntity(entity, request);
        return FlightInstanceMealMapper.toResponse(fimRepository.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public FlightInstanceMealResponse changeStatus(Long id, boolean active) {
        FlightInstanceMeal entity = findOrThrow(id);
        entity.setIsActive(active);
        return FlightInstanceMealMapper.toResponse(fimRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteFlightInstanceMeal(Long id) {
        FlightInstanceMeal entity = findOrThrow(id);
        if (entity.getSoldQuantity() > 0) {
            throw new BadRequestException("Cannot delete: " + entity.getSoldQuantity()
                    + " unit(s) already sold. Deactivate it instead.");
        }
        fimRepository.delete(entity);
        log.info("Deleted flight instance meal id={}", id);
    }

    private FlightInstanceMeal findOrThrow(Long id) {
        return fimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "FlightInstanceMeal", "id", id.toString()));
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDirection) {
        return PageableFactory.of(page, size, sortBy, sortDirection,
                SORTABLE, DEFAULT_SORT_FIELD, Sort.Direction.DESC);
    }
}
