package com.jing.pricing.controller;

import com.jing.common.security.RequireRole;
import com.jing.common.dto.pricing.FareRequest;
import com.jing.common.dto.pricing.FareResponse;
import com.jing.common.pagination.PageResponse;
import com.jing.pricing.payload.filter.FareFilter;
import com.jing.pricing.service.FareService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.jing.common.enumeration.UserRole.ROLE_AIRLINE_OWNER;
import static com.jing.common.enumeration.UserRole.ROLE_SYSTEM_ADMIN;

@RestController
@RequestMapping("/api/v1/fares")
@RequiredArgsConstructor
public class FareController {
    private final FareService fareService;

    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    @PostMapping
    public ResponseEntity<FareResponse> createFare(
            @RequestBody FareRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fareService.createFare(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FareResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(fareService.getById(id));
    }

    /**
     * Filters (all optional): airlineId, flightId, cabinClassId, cabinClass, currency, minPrice, maxPrice.
     * Example: GET /api/v1/fares?flightId=1&minPrice=50&maxPrice=200&sortBy=baseFare&sortDirection=asc
     * Pages are 1-based.
     */
    @GetMapping
    public ResponseEntity<PageResponse<FareResponse>> getAll(
            FareFilter filter,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        return ResponseEntity.ok(fareService.getAll(filter, page, size, sortBy, sortDirection));
    }

    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    @PutMapping("/{id}")
    public ResponseEntity<FareResponse> updateFare(
            @PathVariable Long id,
            @RequestBody FareRequest request) {
        return ResponseEntity.ok(fareService.updateFare(id, request));
    }

    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFare(
            @PathVariable Long id) {
        fareService.deleteFare(id);
        return ResponseEntity.noContent().build();
    }

}
