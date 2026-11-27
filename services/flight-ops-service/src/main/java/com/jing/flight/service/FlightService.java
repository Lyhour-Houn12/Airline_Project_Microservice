package com.jing.flight.service;

import com.jing.common.enumeration.FlightStatus;
import com.jing.common.dto.flight.FlightRequest;
import com.jing.common.dto.flight.FlightResponse;
import com.jing.common.pagination.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface FlightService {
    FlightResponse createFlight(Long userId, FlightRequest request);

    List<FlightResponse> createFlights(Long userId, List<FlightRequest> requests);

    FlightResponse getFlightById(Long id);

    FlightResponse getFlightByNumber(String flightNumber);

    PageResponse<FlightResponse> getFlightsByAirline(Long userId, String keyword, Long airlineId,
                                                     Long aircraftId,
                                                     Long departureAirportId,
                                                     Long arrivalAirportId,
                                                     FlightStatus status,
                                                     int page, int size, String sortBy, String sortDirection);
    FlightResponse updateFlight(Long id, FlightRequest request);

    FlightResponse changeStatus(Long id, FlightStatus status);

    void deleteFlight(Long id);


    Map<Long, FlightResponse> getFlightsByIds(List<Long> ids);
}
