package com.jing.flight.service;

import com.jing.common.security.CurrentUser;
import com.jing.common.enumeration.FlightStatus;
import com.jing.common.dto.flight.FlightRequest;
import com.jing.common.dto.flight.FlightResponse;
import com.jing.common.pagination.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface FlightService {
    FlightResponse createFlight(CurrentUser user, FlightRequest request);

    List<FlightResponse> createFlights(CurrentUser user, List<FlightRequest> requests);

    FlightResponse getFlightById(Long id);

    FlightResponse getFlightByNumber(String flightNumber);

    PageResponse<FlightResponse> getFlightsByAirline(CurrentUser user, String keyword, Long airlineId,
                                                     Long aircraftId,
                                                     Long departureAirportId,
                                                     Long arrivalAirportId,
                                                     FlightStatus status,
                                                     int page, int size, String sortBy, String sortDirection);
    FlightResponse updateFlight(CurrentUser user, Long id, FlightRequest request);

    FlightResponse changeStatus(CurrentUser user, Long id, FlightStatus status);

    void deleteFlight(CurrentUser user, Long id);


    Map<Long, FlightResponse> getFlightsByIds(List<Long> ids);
}
