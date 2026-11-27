package com.jing.seat.controller;

import com.jing.common.dto.seat.SeatMapRequest;
import com.jing.common.dto.seat.SeatMapResponse;
import com.jing.common.pagination.PageResponse;
import com.jing.common.security.CurrentUser;
import com.jing.common.security.RequireRole;
import com.jing.seat.service.SeatMapService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.jing.common.enumeration.UserRole.ROLE_AIRLINE_OWNER;
import static com.jing.common.enumeration.UserRole.ROLE_SYSTEM_ADMIN;

/** Any signed-in user can read seat maps; only system admins and airline owners can change them. */
@RestController
@RequestMapping("/api/v1/seat-maps")
@RequiredArgsConstructor
public class SeatMapController {

    private final SeatMapService seatMapService;

    @PostMapping
    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    public ResponseEntity<SeatMapResponse> createSeatMap(@Valid @RequestBody SeatMapRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(seatMapService.createSeatMap(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SeatMapResponse> getSeatMapById(CurrentUser user, @PathVariable Long id) {
        return ResponseEntity.ok(seatMapService.getSeatMapById(id));
    }

    @GetMapping("/cabin/{cabinId}")
    public ResponseEntity<SeatMapResponse> getSeatMapByCabin(CurrentUser user, @PathVariable Long cabinId) {
        return ResponseEntity.ok(seatMapService.getSeatMapByCabin(cabinId));
    }

    @GetMapping
    public ResponseEntity<PageResponse<SeatMapResponse>> getSeatMaps(
            CurrentUser user,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long cabinId,
            @RequestParam(required = false) Long airlineId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection) {
        return ResponseEntity.ok(seatMapService.getSeatMaps(
                keyword, cabinId, airlineId, page, size, sortBy, sortDirection));
    }

    @PutMapping("/{id}")
    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    public ResponseEntity<SeatMapResponse> updateSeatMap(@PathVariable Long id,
                                                         @Valid @RequestBody SeatMapRequest request) {
        return ResponseEntity.ok(seatMapService.updateSeatMap(id, request));
    }

    @DeleteMapping("/{id}")
    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    public ResponseEntity<Void> deleteSeatMap(@PathVariable Long id) {
        seatMapService.deleteSeatMap(id);   // this call was missing: the endpoint used to delete nothing
        return ResponseEntity.noContent().build();
    }
}
