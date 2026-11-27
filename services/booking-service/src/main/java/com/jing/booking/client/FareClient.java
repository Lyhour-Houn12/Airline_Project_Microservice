package com.jing.booking.client;

import com.jing.common.dto.pricing.FareResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import com.jing.common.pagination.PageResponse;

@FeignClient(name = "pricing-service", contextId = "fareClient")
public interface FareClient {

    @GetMapping("/api/v1/fares/{id}")
    FareResponse getFare(@PathVariable("id") Long id);

    @GetMapping("/api/v1/fares")
    PageResponse<FareResponse> faresOfFlight(@RequestParam("flightId") Long flightId,
                                             @RequestParam("page") int page,
                                             @RequestParam("size") int size);
}
