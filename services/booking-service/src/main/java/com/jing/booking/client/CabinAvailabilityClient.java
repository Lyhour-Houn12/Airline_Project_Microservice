package com.jing.booking.client;

import com.jing.common.dto.seat.FlightInstanceCabinResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/** seat-service's public cabin list of a flight instance (total / held / booked / available places). */
@FeignClient(name = "seat-service", contextId = "cabinAvailabilityClient")
public interface CabinAvailabilityClient {

    @GetMapping("/api/v1/flight-instance-cabins/flight-instance/{flightInstanceId}")
    List<FlightInstanceCabinResponse> cabinsOf(@PathVariable("flightInstanceId") Long flightInstanceId);
}
