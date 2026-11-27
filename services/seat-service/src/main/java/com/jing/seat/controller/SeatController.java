package com.jing.seat.controller;

import com.jing.common.dto.seat.SeatBulkRequest;
import com.jing.common.dto.seat.SeatGenerateRequest;
import com.jing.common.dto.seat.SeatRequest;
import com.jing.common.dto.seat.SeatResponse;
import com.jing.common.dto.seat.SeatUpdateRequest;
import com.jing.common.enumeration.SeatType;
import com.jing.common.pagination.PageResponse;
import com.jing.common.security.RequireRole;
import com.jing.seat.filter.SeatFilter;
import com.jing.seat.service.SeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.jing.common.enumeration.UserRole.ROLE_AIRLINE_OWNER;
import static com.jing.common.enumeration.UserRole.ROLE_SYSTEM_ADMIN;

@RestController
@RequestMapping("/api/v1/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @PostMapping
    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    public ResponseEntity<SeatResponse> create(@Valid @RequestBody SeatRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(seatService.createSeat(request));
    }

    @PostMapping("/bulk")
    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    public ResponseEntity<List<SeatResponse>> createBulk(@Valid @RequestBody SeatBulkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(seatService.createSeats(request));
    }

    @PostMapping("/generate")
    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    public ResponseEntity<List<SeatResponse>> generate(@Valid @RequestBody SeatGenerateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(seatService.generateSeats(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SeatResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(seatService.getById(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<SeatResponse>> search(
            @RequestParam(required = false) Long seatMapId,
            @RequestParam(required = false) Long aircraftId,
            @RequestParam(required = false) SeatType seatType,
            @RequestParam(required = false) Integer seatRow,
            @RequestParam(required = false) Character columnLetter,
            @RequestParam(required = false) Boolean isAvailable,
            @RequestParam(required = false) Boolean isBlocked,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Boolean isEmergencyExit,
            @RequestParam(required = false) Boolean isWheelchairAccessible,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {
        SeatFilter filter = new SeatFilter(seatMapId, aircraftId, seatType, seatRow, columnLetter,
                isAvailable, isBlocked, isActive, isEmergencyExit, isWheelchairAccessible);
        return ResponseEntity.ok(seatService.search(filter, page, size, sortBy, sortDirection));
    }

    @PutMapping("/{id}")
    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    public ResponseEntity<SeatResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody SeatUpdateRequest request) {
        return ResponseEntity.ok(seatService.updateSeat(id, request));
    }

    @PatchMapping("/{id}/block")
    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    public ResponseEntity<SeatResponse> block(@PathVariable Long id) {
        return ResponseEntity.ok(seatService.blockSeat(id));
    }

    @PatchMapping("/{id}/unblock")
    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    public ResponseEntity<SeatResponse> unblock(@PathVariable Long id) {
        return ResponseEntity.ok(seatService.unblockSeat(id));
    }

    @PatchMapping("/{id}/activate")
    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    public ResponseEntity<SeatResponse> activate(@PathVariable Long id) {
        return ResponseEntity.ok(seatService.setActive(id, true));
    }

    @PatchMapping("/{id}/deactivate")
    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    public ResponseEntity<SeatResponse> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(seatService.setActive(id, false));
    }

    @DeleteMapping("/{id}")
    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        seatService.deleteSeat(id);
        return ResponseEntity.noContent().build();
    }
}
