package com.jing.pricing.controller;

import com.jing.common.security.RequireRole;
import com.jing.common.dto.pricing.FareRulesRequest;
import com.jing.common.dto.pricing.FareRulesResponse;
import com.jing.pricing.service.FareRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.jing.common.enumeration.UserRole.ROLE_AIRLINE_OWNER;
import static com.jing.common.enumeration.UserRole.ROLE_SYSTEM_ADMIN;

@RestController
@RequestMapping("/api/v1/fare-rules")
@RequiredArgsConstructor
public class FareRuleController {

    private final FareRuleService fareRuleService;

    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    @PostMapping
    public ResponseEntity<FareRulesResponse> create(
            @RequestBody FareRulesRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fareRuleService.createFareRule(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FareRulesResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(fareRuleService.getById(id));
    }

    @GetMapping("/fare/{fareId}")
    public ResponseEntity<FareRulesResponse> getByFareId(@PathVariable Long fareId) {
        return ResponseEntity.ok(fareRuleService.getByFareId(fareId));
    }

    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    @PutMapping("/{id}")
    public ResponseEntity<FareRulesResponse> update(
            @PathVariable Long id,
            @RequestBody FareRulesRequest request) {
        return ResponseEntity.ok(fareRuleService.updateFareRule(id, request));
    }

    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id) {
        fareRuleService.deleteFareRule(id);
        return ResponseEntity.noContent().build();
    }
}
