package com.jing.airline.controller;

import com.jing.airline.service.AirlineService;
import com.jing.common.dto.airline.AirlineDropDownItem;
import com.jing.common.dto.airline.AirlineRequest;
import com.jing.common.dto.airline.AirlineResponse;
import com.jing.common.enumeration.AirlineStatus;
import com.jing.common.pagination.PageResponse;
import com.jing.common.security.CurrentUser;
import com.jing.common.security.RequireRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.jing.common.enumeration.UserRole.ROLE_AIRLINE_OWNER;
import static com.jing.common.enumeration.UserRole.ROLE_SYSTEM_ADMIN;

@RestController
@RequestMapping("/api/v1/airlines")
@RequiredArgsConstructor
public class AirlineController {

    private final AirlineService airlineService;

    @PostMapping
    @RequireRole(ROLE_AIRLINE_OWNER)
    public ResponseEntity<AirlineResponse> createAirline(CurrentUser user,
                                                         @Valid @RequestBody AirlineRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(airlineService.createAirline(request, user.id()));
    }

    @GetMapping("/me")
    @RequireRole(ROLE_AIRLINE_OWNER)
    public ResponseEntity<AirlineResponse> getMyAirline(CurrentUser user) {
        return ResponseEntity.ok(airlineService.getAirlineByOwner(user.id()));
    }

    @PutMapping("/me")
    @RequireRole(ROLE_AIRLINE_OWNER)
    public ResponseEntity<AirlineResponse> updateMyAirline(CurrentUser user,
                                                           @Valid @RequestBody AirlineRequest request) {
        return ResponseEntity.ok(airlineService.updateAirline(request, user.id()));
    }

    @DeleteMapping("/{id}")
    @RequireRole(ROLE_AIRLINE_OWNER)
    public ResponseEntity<Void> deleteAirline(CurrentUser user, @PathVariable Long id) {
        airlineService.deleteAirline(id, user.id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<PageResponse<AirlineResponse>> getAllAirlines(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "airlineName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {
        return ResponseEntity.ok(airlineService.getAllAirlines(keyword, page, size, sortBy, sortDirection));
    }

    @GetMapping("/dropdown")
    public ResponseEntity<List<AirlineDropDownItem>> getAirlinesForDropdown() {
        return ResponseEntity.ok(airlineService.getAirlinesForDropdown());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AirlineResponse> getAirlineById(@PathVariable Long id) {
        return ResponseEntity.ok(airlineService.getAirlineById(id));
    }

    @PatchMapping("/{id}/status")
    @RequireRole(ROLE_SYSTEM_ADMIN)
    public ResponseEntity<AirlineResponse> changeStatus(@PathVariable Long id,
                                                        @RequestParam AirlineStatus status) {
        return ResponseEntity.ok(airlineService.changeStatusByAdmin(id, status));
    }
}
