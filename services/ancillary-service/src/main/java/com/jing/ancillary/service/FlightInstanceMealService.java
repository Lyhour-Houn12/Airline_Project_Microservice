package com.jing.ancillary.service;

import com.jing.ancillary.filter.FlightInstanceMealFilter;
import com.jing.common.dto.ancillary.FlightInstanceMealBulkRequest;
import com.jing.common.dto.ancillary.FlightInstanceMealUpdateRequest;
import com.jing.common.dto.ancillary.FlightInstanceMealResponse;
import com.jing.common.pagination.PageResponse;

import java.util.List;

public interface FlightInstanceMealService {
    List<FlightInstanceMealResponse> createFlightInstanceMeals(FlightInstanceMealBulkRequest request);

    FlightInstanceMealResponse getById(Long id);

    PageResponse<FlightInstanceMealResponse> getAll(FlightInstanceMealFilter filter, int page, int size,
                                                    String sortBy, String sortDirection);

    FlightInstanceMealResponse updateFlightInstanceMeal(Long id, FlightInstanceMealUpdateRequest request);

    FlightInstanceMealResponse changeStatus(Long id, boolean active);

    void deleteFlightInstanceMeal(Long id);
}
