package com.jing.ancillary.controller;

import com.jing.common.security.RequireRole;
import com.jing.common.security.AuthUser;
import com.jing.common.security.CurrentUser;
import com.jing.ancillary.filter.FlightCabinAncillaryFilter;
import com.jing.ancillary.service.FlightCabinAncillaryService;
import com.jing.common.enumeration.AncillaryType;
import com.jing.common.dto.ancillary.FlightCabinAncillaryBulkRequest;
import com.jing.common.dto.ancillary.FlightCabinAncillaryUpdateRequest;
import com.jing.common.dto.ancillary.FlightCabinAncillaryResponse;
import com.jing.common.pagination.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.jing.common.enumeration.UserRole.ROLE_AIRLINE_OWNER;
import static com.jing.common.enumeration.UserRole.ROLE_SYSTEM_ADMIN;

@RestController
@RequestMapping("/api/v1/flight-cabin-ancillaries")
@RequiredArgsConstructor
public class FlightCabinAncillaryController {

    private final FlightCabinAncillaryService fcaService;

    // assign one or many ancillaries to a flight instance cabin
    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    @PostMapping
    public ResponseEntity<List<FlightCabinAncillaryResponse>> create(
            @Valid @RequestBody FlightCabinAncillaryBulkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fcaService.createFlightCabinAncillaries(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FlightCabinAncillaryResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(fcaService.getById(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<FlightCabinAncillaryResponse>> getAll(
            @AuthUser(required = false) CurrentUser user,
            @RequestParam(required = false) Long flightInstanceCabinId,
            @RequestParam(required = false) Long ancillaryId,
            @RequestParam(required = false) AncillaryType ancillaryType,
            @RequestParam(required = false) Boolean isFree,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        boolean publicView = !user.isStaff();

        FlightCabinAncillaryFilter filter = new FlightCabinAncillaryFilter(
                flightInstanceCabinId, ancillaryId, ancillaryType, isFree,
                publicView ? null : isActive, inStock, publicView);

        return ResponseEntity.ok(fcaService.getAll(filter, page, size, sortBy, sortDirection));
    }

    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    @PutMapping("/{id}")
    public ResponseEntity<FlightCabinAncillaryResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody FlightCabinAncillaryUpdateRequest request) {
        return ResponseEntity.ok(fcaService.updateFlightCabinAncillary(id, request));
    }

    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    @PatchMapping("/{id}/status")
    public ResponseEntity<FlightCabinAncillaryResponse> changeStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {
        return ResponseEntity.ok(fcaService.changeStatus(id, active));
    }

    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id) {
        fcaService.deleteFlightCabinAncillary(id);
        return ResponseEntity.noContent().build();
    }
}
