package com.jing.common.airline;

import com.jing.common.dto.airline.AircraftResponse;
import com.jing.common.dto.airline.AirlineResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * airline-core-service, for {@link AirlineAccess}. A service that uses it adds
 * {@code com.jing.common.airline} to its {@code @EnableFeignClients} packages.
 */
@FeignClient(name = "airline-core-service", contextId = "airlineCoreClient")
public interface AirlineCoreClient {

    @GetMapping("/internal/v1/airlines/by-owner/{ownerId}")
    AirlineResponse airlineOfOwner(@PathVariable("ownerId") Long ownerId);

    @GetMapping("/api/v1/airlines/{id}")
    AirlineResponse airline(@PathVariable("id") Long id);

    @GetMapping("/api/v1/aircrafts/{id}")
    AircraftResponse aircraft(@PathVariable("id") Long id);
}
