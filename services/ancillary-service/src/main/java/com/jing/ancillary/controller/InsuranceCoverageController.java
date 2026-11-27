package com.jing.ancillary.controller;

import com.jing.common.security.RequireRole;
import com.jing.common.security.AuthUser;
import com.jing.common.security.CurrentUser;
import com.jing.ancillary.filter.InsuranceCoverageFilter;
import com.jing.ancillary.service.InsuranceService;
import com.jing.common.enumeration.CoverageType;
import com.jing.common.dto.ancillary.InsuranceCoverageRequest;
import com.jing.common.dto.ancillary.InsuranceCoverageResponse;
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
@RequestMapping("/api/v1/insurance-coverages")
@RequiredArgsConstructor
public class InsuranceCoverageController {

    private final InsuranceService coverageService;

    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    @PostMapping
    public ResponseEntity<InsuranceCoverageResponse> create(
            @Valid @RequestBody InsuranceCoverageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(coverageService.createCoverage(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InsuranceCoverageResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(coverageService.getById(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<InsuranceCoverageResponse>> getAll(
            @AuthUser(required = false) CurrentUser user,
            @RequestParam(required = false) Long airlineId,
            @RequestParam(required = false) CoverageType coverageType,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer eligibleAge,
            @RequestParam(required = false) BigDecimal minPremium,
            @RequestParam(required = false) BigDecimal maxPremium,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        // customers / anonymous callers only see active coverages
        Boolean activeFilter = user.isStaff() ? isActive : Boolean.TRUE;

        InsuranceCoverageFilter filter = new InsuranceCoverageFilter(
                airlineId, coverageType, provider, currency, activeFilter,
                keyword, eligibleAge, minPremium, maxPremium);

        return ResponseEntity.ok(
                coverageService.getAll(filter, page, size, sortBy, sortDirection));
    }

    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    @PutMapping("/{id}")
    public ResponseEntity<InsuranceCoverageResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody InsuranceCoverageRequest request) {
        return ResponseEntity.ok(coverageService.updateCoverage(id, request));
    }

    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    @PatchMapping("/{id}/status")
    public ResponseEntity<InsuranceCoverageResponse> changeStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {
        return ResponseEntity.ok(coverageService.changeStatus(id, active));
    }

    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id) {
        coverageService.deleteCoverage(id);
        return ResponseEntity.noContent().build();
    }
}
