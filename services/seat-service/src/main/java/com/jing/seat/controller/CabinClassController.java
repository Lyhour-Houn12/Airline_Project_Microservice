package com.jing.seat.controller;

import com.jing.common.dto.seat.CabinClassRequest;
import com.jing.common.dto.seat.CabinClassResponse;
import com.jing.common.enumeration.CabinClassType;
import com.jing.common.pagination.PageResponse;
import com.jing.common.security.RequireRole;
import com.jing.seat.filter.CabinClassSearchRequest;
import com.jing.seat.service.CabinClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.jing.common.enumeration.UserRole.ROLE_AIRLINE_OWNER;
import static com.jing.common.enumeration.UserRole.ROLE_SYSTEM_ADMIN;

@RestController
@RequestMapping("/api/v1/cabin-classes")
@RequiredArgsConstructor
public class CabinClassController {

    private final CabinClassService cabinClassService;

    @PostMapping
    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    public ResponseEntity<CabinClassResponse> create(@RequestBody CabinClassRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cabinClassService.createCabinClass(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CabinClassResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(cabinClassService.getCabinClassById(id));
    }

    /**
     * Filters (all optional): aircraftId, cabinClassType, isActive, isBookable, code, keyword (name or code).
     * Example: GET /api/v1/cabin-classes?aircraftId=1&keyword=econ&sortBy=displayOrder&sortDirection=asc
     * Pages are 1-based.
     */
    @GetMapping
    public ResponseEntity<PageResponse<CabinClassResponse>> getAll(
            CabinClassSearchRequest searchRequest,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "displayOrder") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {
        return ResponseEntity.ok(
                cabinClassService.getAllCabinClasses(searchRequest, page, size, sortBy, sortDirection));
    }

    @GetMapping("/aircraft/{aircraftId}/type/{type}")
    public ResponseEntity<CabinClassResponse> getByAircraftIdAndType(
            @PathVariable Long aircraftId,
            @PathVariable("type") CabinClassType cabinClassType) {
        return ResponseEntity.ok(cabinClassService.getCabinClassByAircraftIdAndType(aircraftId, cabinClassType));
    }

    @PutMapping("/{id}")
    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    public ResponseEntity<CabinClassResponse> update(@PathVariable Long id,
                                                     @RequestBody CabinClassRequest request) {
        return ResponseEntity.ok(cabinClassService.updateCabinClass(id, request));
    }

    @DeleteMapping("/{id}")
    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        cabinClassService.deleteCabinClass(id);
        return ResponseEntity.noContent().build();
    }
}
