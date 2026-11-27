package com.jing.ancillary.client;

import com.jing.common.dto.seat.FlightInstanceCabinResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "seat-service", contextId = "ancillaryFlightInstanceCabinClient")
public interface FlightInstanceCabinClient {

    @GetMapping("/api/v1/flight-instance-cabins/{id}")
    FlightInstanceCabinResponse flightInstanceCabin(@PathVariable("id") Long id);
}
