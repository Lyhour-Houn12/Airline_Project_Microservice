package com.jing.location.controller;

import com.jing.common.dto.location.AirportRequest;
import com.jing.common.dto.location.AirportResponse;
import com.jing.common.pagination.PageResponse;
import com.jing.common.security.RequireRole;
import com.jing.location.service.AirportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.jing.common.enumeration.UserRole.ROLE_SYSTEM_ADMIN;

/** Airports are shared reference data: everyone can read them, only system admins can change them. */
@RestController
@RequestMapping("/api/v1/airports")
@RequiredArgsConstructor
@Validated
public class AirportController {

    private final AirportService airportService;

    // "/create" is kept so existing clients keep working; prefer POST /api/v1/airports
    @PostMapping({"", "/create"})
    @RequireRole(ROLE_SYSTEM_ADMIN)
    public ResponseEntity<AirportResponse> createAirport(@Valid @RequestBody AirportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(airportService.createAirport(request));
    }

    @PostMapping("/bulk")
    @RequireRole(ROLE_SYSTEM_ADMIN)
    public ResponseEntity<List<AirportResponse>> createBulkAirports(
            @Valid @RequestBody List<AirportRequest> requests) {
        return ResponseEntity.status(HttpStatus.CREATED).body(airportService.createBulkAirports(requests));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AirportResponse> getAirportById(@PathVariable Long id) {
        return ResponseEntity.ok(airportService.getAirportById(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<AirportResponse>> getAllAirports(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {
        return ResponseEntity.ok(airportService.getAllAirports(keyword, page, size, sortBy, sortDirection));
    }

    @GetMapping("/city/{cityId}")
    public ResponseEntity<List<AirportResponse>> getAirportsByCityId(@PathVariable Long cityId) {
        return ResponseEntity.ok(airportService.getAirportsByCityId(cityId));
    }

    @PutMapping("/{id}")
    @RequireRole(ROLE_SYSTEM_ADMIN)
    public ResponseEntity<AirportResponse> updateAirport(@PathVariable Long id,
                                                         @Valid @RequestBody AirportRequest request) {
        return ResponseEntity.ok(airportService.updateAirport(id, request));
    }

    @DeleteMapping("/{id}")
    @RequireRole(ROLE_SYSTEM_ADMIN)
    public ResponseEntity<Void> deleteAirport(@PathVariable Long id) {
        airportService.deleteAirport(id);
        return ResponseEntity.noContent().build();
    }
}
