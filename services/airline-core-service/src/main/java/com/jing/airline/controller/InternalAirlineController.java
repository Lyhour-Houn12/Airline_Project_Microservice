package com.jing.airline.controller;

import com.jing.airline.service.AirlineService;
import com.jing.common.dto.airline.AirlineResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Used by the other services to enforce ownership ("which airline does this owner run?").
 * {@code /internal/**} is service-to-service only: the API gateway never routes it.
 */
@RestController
@RequestMapping("/internal/v1/airlines")
@RequiredArgsConstructor
public class InternalAirlineController {

    private final AirlineService airlineService;

    /** 404 when this user owns no airline. */
    @GetMapping("/by-owner/{ownerId}")
    public AirlineResponse byOwner(@PathVariable Long ownerId) {
        return airlineService.getAirlineByOwner(ownerId);
    }
}
