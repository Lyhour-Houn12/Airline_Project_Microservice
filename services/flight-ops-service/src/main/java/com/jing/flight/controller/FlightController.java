package com.jing.flight.controller;

import com.jing.common.dto.flight.FlightRequest;
import com.jing.common.dto.flight.FlightResponse;
import com.jing.common.enumeration.FlightStatus;
import com.jing.common.pagination.PageResponse;
import com.jing.common.security.CurrentUser;
import com.jing.common.security.RequireRole;
import com.jing.flight.service.FlightService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.jing.common.enumeration.UserRole.ROLE_AIRLINE_OWNER;
import static com.jing.common.enumeration.UserRole.ROLE_SYSTEM_ADMIN;

@RestController
@RequestMapping("/api/v1/flights")
@RequiredArgsConstructor
@Validated
public class FlightController {

    private final FlightService flightService;

    @PostMapping
    @RequireRole(ROLE_AIRLINE_OWNER)
    public ResponseEntity<FlightResponse> createFlight(CurrentUser user,
                                                       @Valid @RequestBody FlightRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(flightService.createFlight(user, request));
    }

    @PostMapping("/batch")
    @RequireRole(ROLE_AIRLINE_OWNER)
    public ResponseEntity<List<FlightResponse>> createFlights(
            CurrentUser user,
            @RequestBody @NotEmpty @Size(max = 100) List<@Valid FlightRequest> requests) {
        return ResponseEntity.status(HttpStatus.CREATED).body(flightService.createFlights(user, requests));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FlightResponse> getFlightById(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(flightService.getFlightById(id));
    }

    @GetMapping("/number/{flightNumber}")
    public ResponseEntity<FlightResponse> getFlightByNumber(@PathVariable @NotBlank String flightNumber) {
        return ResponseEntity.ok(flightService.getFlightByNumber(flightNumber));
    }

    /** Any signed-in user can search flights. */
    @GetMapping
    public ResponseEntity<PageResponse<FlightResponse>> getFlights(
            CurrentUser user,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @Positive Long airlineId,
            @RequestParam(required = false) @Positive Long aircraftId,
            @RequestParam(required = false) @Positive Long departureAirportId,
            @RequestParam(required = false) @Positive Long arrivalAirportId,
            @RequestParam(required = false) FlightStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        // airline owners only see their own airline's flights (enforced in the service)
        return ResponseEntity.ok(flightService.getFlightsByAirline(
                user, keyword, airlineId, aircraftId, departureAirportId, arrivalAirportId,
                status, page, size, sortBy, sortDirection));
    }

    /** Internal (service-to-service) only: do not expose through the gateway. */
    @GetMapping("/by-ids")
    public ResponseEntity<Map<Long, FlightResponse>> getFlightsByIds(
            @RequestParam @NotEmpty @Size(max = 200) List<@NotNull @Positive Long> ids) {
        return ResponseEntity.ok(flightService.getFlightsByIds(ids));
    }

    // an owner may only touch flights of their own airline (enforced in the service)
    @PutMapping("/{id}")
    @RequireRole({ROLE_AIRLINE_OWNER, ROLE_SYSTEM_ADMIN})
    public ResponseEntity<FlightResponse> updateFlight(CurrentUser user, @PathVariable @Positive Long id,
                                                       @Valid @RequestBody FlightRequest request) {
        return ResponseEntity.ok(flightService.updateFlight(user, id, request));
    }

    @PatchMapping("/{id}/status")
    @RequireRole({ROLE_AIRLINE_OWNER, ROLE_SYSTEM_ADMIN})
    public ResponseEntity<FlightResponse> changeStatus(CurrentUser user, @PathVariable @Positive Long id,
                                                       @RequestParam @NotNull FlightStatus status) {
        return ResponseEntity.ok(flightService.changeStatus(user, id, status));
    }

    @DeleteMapping("/{id}")
    @RequireRole({ROLE_AIRLINE_OWNER, ROLE_SYSTEM_ADMIN})
    public ResponseEntity<Void> deleteFlight(CurrentUser user, @PathVariable @Positive Long id) {
        flightService.deleteFlight(user, id);
        return ResponseEntity.noContent().build();
    }
}
