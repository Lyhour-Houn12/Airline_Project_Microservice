package com.jing.booking.client;

import com.jing.common.dto.flight.FlightInstanceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import com.jing.common.pagination.PageResponse;

@FeignClient(name = "flight-ops-service", contextId = "flightInstanceClient")
public interface FlightInstanceClient {

    @GetMapping("/api/v1/flight-instances/{id}")
    FlightInstanceResponse getFlightInstance(@PathVariable("id") Long id);

    /** The public (customer) list: only scheduled, active, upcoming flights. */
    @GetMapping("/api/v1/flight-instances")
    PageResponse<FlightInstanceResponse> search(@RequestParam("departureAirportId") Long departureAirportId,
                                                @RequestParam("arrivalAirportId") Long arrivalAirportId,
                                                @RequestParam("onDate") String onDate,
                                                @RequestParam("page") int page,
                                                @RequestParam("size") int size,
                                                @RequestParam("sortBy") String sortBy,
                                                @RequestParam("sortDirection") String sortDirection);
}
