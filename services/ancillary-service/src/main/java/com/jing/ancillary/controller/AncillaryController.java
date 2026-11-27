package com.jing.ancillary.controller;

import com.jing.ancillary.service.AncillaryOwnership;
import com.jing.common.security.RequireRole;
import com.jing.common.security.AuthUser;
import com.jing.common.security.CurrentUser;
import com.jing.ancillary.filter.AncillaryFilter;
import com.jing.ancillary.service.AncillaryService;
import com.jing.common.enumeration.AncillaryType;
import com.jing.common.dto.ancillary.AncillaryRequest;
import com.jing.common.dto.ancillary.AncillaryResponse;
import com.jing.common.pagination.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

import static com.jing.common.enumeration.UserRole.ROLE_AIRLINE_OWNER;
import static com.jing.common.enumeration.UserRole.ROLE_SYSTEM_ADMIN;

@RestController
@RequestMapping("/api/v1/ancillaries")
@RequiredArgsConstructor
public class AncillaryController {

    private final AncillaryService ancillaryService;
    private final AncillaryOwnership ownership;

    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    @PostMapping
    public ResponseEntity<AncillaryResponse> create(
            CurrentUser user,
            @Valid @RequestBody AncillaryRequest request) {
        ownership.newRecord(user, request.airlineId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ancillaryService.createAncillary(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AncillaryResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ancillaryService.getById(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<AncillaryResponse>> getAll(
            @AuthUser(required = false) CurrentUser user,
            @RequestParam(required = false) Long airlineId,
            @RequestParam(required = false) AncillaryType type,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        // customers / anonymous callers only see active ancillaries
        Boolean activeFilter = user.isStaff() ? isActive : Boolean.TRUE;

        AncillaryFilter filter = new AncillaryFilter(
                airlineId, type, currency, activeFilter, keyword, minPrice, maxPrice);

        return ResponseEntity.ok(
                ancillaryService.getAll(filter, page, size, sortBy, sortDirection));
    }

    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    @PutMapping("/{id}")
    public ResponseEntity<AncillaryResponse> update(
            CurrentUser user,
            @PathVariable Long id,
            @Valid @RequestBody AncillaryRequest request) {
        ownership.ancillary(user, id);
        return ResponseEntity.ok(ancillaryService.updateAncillary(id, request));
    }

    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    @PatchMapping("/{id}/status")
    public ResponseEntity<AncillaryResponse> changeStatus(
            CurrentUser user,
            @PathVariable Long id,
            @RequestParam boolean active) {
        ownership.ancillary(user, id);
        return ResponseEntity.ok(ancillaryService.changeStatus(id, active));
    }

    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            CurrentUser user,
            @PathVariable Long id) {
        ownership.ancillary(user, id);
        ancillaryService.deleteAncillary(id);
        return ResponseEntity.noContent().build();
    }
}
