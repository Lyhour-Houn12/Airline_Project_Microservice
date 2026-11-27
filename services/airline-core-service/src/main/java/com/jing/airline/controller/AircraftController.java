package com.jing.airline.controller;

import com.jing.airline.service.AircraftService;
import com.jing.common.dto.airline.AircraftRequest;
import com.jing.common.dto.airline.AircraftResponse;
import com.jing.common.enumeration.AircraftStatus;
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
@RequestMapping("/api/v1/aircrafts")
@RequiredArgsConstructor
public class AircraftController {

    private final AircraftService aircraftService;

    @PostMapping
    @RequireRole(ROLE_AIRLINE_OWNER)
    public ResponseEntity<AircraftResponse> createAircraft(CurrentUser user,
                                                           @Valid @RequestBody AircraftRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(aircraftService.createAircraft(request, user.id()));
    }

    @GetMapping("/me")
    @RequireRole(ROLE_AIRLINE_OWNER)
    public ResponseEntity<List<AircraftResponse>> getMyAircraft(CurrentUser user) {
        return ResponseEntity.ok(aircraftService.listAllAircraftByOwner(user.id()));
    }

    @PutMapping("/{id}")
    @RequireRole(ROLE_AIRLINE_OWNER)
    public ResponseEntity<AircraftResponse> updateAircraft(CurrentUser user,
                                                           @PathVariable Long id,
                                                           @Valid @RequestBody AircraftRequest request) {
        return ResponseEntity.ok(aircraftService.updateAircraft(id, request, user.id()));
    }

    @DeleteMapping("/{id}")
    @RequireRole(ROLE_AIRLINE_OWNER)
    public ResponseEntity<Void> deleteAircraft(CurrentUser user, @PathVariable Long id) {
        aircraftService.deleteAircraft(id, user.id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<PageResponse<AircraftResponse>> getAllAircraft(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "code") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {
        return ResponseEntity.ok(aircraftService.getAllAircraft(keyword, page, size, sortBy, sortDirection));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AircraftResponse> getAircraftById(@PathVariable Long id) {
        return ResponseEntity.ok(aircraftService.getAircraftById(id));
    }

    @PatchMapping("/{id}/status")
    @RequireRole(ROLE_SYSTEM_ADMIN)
    public ResponseEntity<AircraftResponse> changeStatus(@PathVariable Long id,
                                                         @RequestParam AircraftStatus status) {
        return ResponseEntity.ok(aircraftService.changeStatusByAdmin(id, status));
    }

    @DeleteMapping("/admin/{id}")
    @RequireRole(ROLE_SYSTEM_ADMIN)
    public ResponseEntity<Void> deleteAircraftByAdmin(@PathVariable Long id) {
        aircraftService.deleteAircraftByAdmin(id);
        return ResponseEntity.noContent().build();
    }
}
