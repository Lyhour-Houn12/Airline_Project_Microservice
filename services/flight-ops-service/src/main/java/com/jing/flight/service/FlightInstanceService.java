package com.jing.flight.service;

import com.jing.common.enumeration.FlightStatus;
import com.jing.common.dto.flight.FlightInstanceRequest;
import com.jing.common.dto.flight.FlightInstanceResponse;
import com.jing.common.pagination.PageResponse;
import com.jing.flight.filter.FlightInstanceFilter;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface FlightInstanceService {
    FlightInstanceResponse createFlightInstanceWithCabins(Long userId, FlightInstanceRequest request);

    /**
     * Replaces getFlightInstance + getByAirlineId.
     * publicView = true for customers: only bookable instances (SCHEDULED, active, in the future, with free seats).
     */
    PageResponse<FlightInstanceResponse> getFlightInstances(FlightInstanceFilter filter,
                                                            boolean publicView,
                                                            int page,
                                                            int size,
                                                            String sortBy,
                                                            String sortDirection);

    FlightInstanceResponse getFlightInstanceById(Long id);

    FlightInstanceResponse updateFlightInstance(Long id, FlightInstanceRequest request);

    FlightInstanceResponse changeStatus(Long id, FlightStatus status);

    void deleteFlightInstance(Long id);

    Map<Long, FlightInstanceResponse> getFlightInstancesByIds(List<Long> ids);



}
