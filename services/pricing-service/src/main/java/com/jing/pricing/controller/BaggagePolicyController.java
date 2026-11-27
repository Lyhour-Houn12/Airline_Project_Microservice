package com.jing.pricing.controller;

import com.jing.common.security.RequireRole;
import com.jing.common.dto.pricing.BaggagePolicyRequest;
import com.jing.common.dto.pricing.BaggagePolicyResponse;
import com.jing.pricing.service.BaggagePolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.jing.common.enumeration.UserRole.ROLE_AIRLINE_OWNER;
import static com.jing.common.enumeration.UserRole.ROLE_SYSTEM_ADMIN;

@RestController
@RequestMapping("/api/v1/baggage-policies")
@RequiredArgsConstructor
public class BaggagePolicyController {

    private final BaggagePolicyService baggagePolicyService;

    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    @PostMapping
    public ResponseEntity<BaggagePolicyResponse> create(
            @RequestBody BaggagePolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(baggagePolicyService.createBaggagePolicy(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaggagePolicyResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(baggagePolicyService.getById(id));
    }

    @GetMapping("/fare/{fareId}")
    public ResponseEntity<BaggagePolicyResponse> getByFareId(@PathVariable Long fareId) {
        return ResponseEntity.ok(baggagePolicyService.getByFareId(fareId));
    }

    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    @PutMapping("/{id}")
    public ResponseEntity<BaggagePolicyResponse> update(
            @PathVariable Long id,
            @RequestBody BaggagePolicyRequest request) {
        return ResponseEntity.ok(baggagePolicyService.updateBaggagePolicy(id, request));
    }

    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id) {
        baggagePolicyService.deleteBaggagePolicy(id);
        return ResponseEntity.noContent().build();
    }
}
