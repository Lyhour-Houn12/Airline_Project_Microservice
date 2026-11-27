package com.jing.pricing.client;

import com.jing.common.dto.flight.FlightResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "flight-ops-service", contextId = "pricingFlightClient")
public interface FlightOpsClient {

    @GetMapping("/api/v1/flights/{id}")
    FlightResponse flight(@PathVariable("id") Long id);
}
