package com.jing.booking.client;

import com.jing.common.dto.flight.FlightInstanceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "flight-ops-service", contextId = "flightInstanceClient")
public interface FlightInstanceClient {

    @GetMapping("/api/v1/flight-instances/{id}")
    FlightInstanceResponse getFlightInstance(@PathVariable("id") Long id);
}
