package com.jing.pricing.client;

import com.jing.common.dto.seat.CabinClassResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "seat-service", contextId = "pricingCabinClassClient")
public interface CabinClassClient {

    @GetMapping("/api/v1/cabin-classes/{id}")
    CabinClassResponse cabinClass(@PathVariable("id") Long id);
}
