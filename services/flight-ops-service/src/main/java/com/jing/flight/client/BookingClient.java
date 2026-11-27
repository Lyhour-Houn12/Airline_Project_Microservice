package com.jing.flight.client;

import com.jing.common.dto.booking.FlightCancelledRequest;
import com.jing.common.dto.booking.FlightCancelledResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** booking-service's internal API (not reachable through the gateway). */
@FeignClient(name = "booking-service", contextId = "flightOpsBookingClient")
public interface BookingClient {

    @PostMapping("/internal/v1/flight-instances/{id}/cancelled")
    FlightCancelledResponse flightCancelled(@PathVariable("id") Long flightInstanceId,
                                            @RequestBody FlightCancelledRequest request);
}
