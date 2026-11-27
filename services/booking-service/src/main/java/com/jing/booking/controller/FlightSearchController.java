package com.jing.booking.controller;

import com.jing.booking.payload.response.FlightSearchResponse;
import com.jing.booking.service.FlightSearchService;
import com.jing.common.enumeration.CabinClassType;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Public: anyone can search, no login needed.
 * <pre>GET /api/v1/flight-search?from=1&amp;to=4&amp;date=2026-12-24&amp;adults=2&amp;children=1&amp;infants=0&amp;cabin=ECONOMY&amp;sort=PRICE</pre>
 */
@RestController
@RequestMapping("/api/v1/flight-search")
@RequiredArgsConstructor
public class FlightSearchController {

    private final FlightSearchService flightSearchService;

    @GetMapping
    public FlightSearchResponse search(
            @RequestParam Long from,
            @RequestParam Long to,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "1") int adults,
            @RequestParam(defaultValue = "0") int children,
            @RequestParam(defaultValue = "0") int infants,
            @RequestParam(required = false) CabinClassType cabin,
            @RequestParam(required = false) String currency,
            @RequestParam(defaultValue = "DEPARTURE") FlightSearchService.Sort sort) {
        return flightSearchService.search(from, to, date, adults, children, infants, cabin, currency, sort);
    }
}
