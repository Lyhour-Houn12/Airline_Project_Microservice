package com.jing.flight.client;


import com.jing.common.dto.location.AirportResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "location-service", contextId = "airportClient")
public interface AirportClient {
    @GetMapping("/api/v1/airports/{id}")
    AirportResponse getAirportById(@PathVariable("id") Long id);
}
