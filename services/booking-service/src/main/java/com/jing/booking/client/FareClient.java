package com.jing.booking.client;

import com.jing.common.dto.pricing.FareResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "pricing-service", contextId = "fareClient")
public interface FareClient {

    @GetMapping("/api/v1/fares/{id}")
    FareResponse getFare(@PathVariable("id") Long id);
}
