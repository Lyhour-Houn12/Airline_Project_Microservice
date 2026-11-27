package com.jing.ancillary.controller;

import com.jing.ancillary.service.AncillaryOwnership;
import com.jing.common.security.RequireRole;
import com.jing.common.security.AuthUser;
import com.jing.common.security.CurrentUser;
import com.jing.ancillary.filter.FlightInstanceMealFilter;
import com.jing.ancillary.service.FlightInstanceMealService;
import com.jing.common.enumeration.DietaryType;
import com.jing.common.enumeration.MealType;
import com.jing.common.dto.ancillary.FlightInstanceMealBulkRequest;
import com.jing.common.dto.ancillary.FlightInstanceMealUpdateRequest;
import com.jing.common.dto.ancillary.FlightInstanceMealResponse;
import com.jing.common.pagination.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.jing.common.enumeration.UserRole.ROLE_AIRLINE_OWNER;
import static com.jing.common.enumeration.UserRole.ROLE_SYSTEM_ADMIN;

@RestController
@RequestMapping("/api/v1/flight-instance-meals")
@RequiredArgsConstructor
public class FlightInstanceMealController {

    private final FlightInstanceMealService fimService;
    private final AncillaryOwnership ownership;

    // assign one or many meals to a flight instance
    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    @PostMapping
    public ResponseEntity<List<FlightInstanceMealResponse>> create(
            CurrentUser user,
            @Valid @RequestBody FlightInstanceMealBulkRequest request) {
        ownership.newFlightInstanceMeals(user, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fimService.createFlightInstanceMeals(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FlightInstanceMealResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(fimService.getById(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<FlightInstanceMealResponse>> getAll(
            @AuthUser(required = false) CurrentUser user,
            @RequestParam(required = false) Long flightInstanceId,
            @RequestParam(required = false) Long mealId,
            @RequestParam(required = false) MealType mealType,
            @RequestParam(required = false) DietaryType dietaryType,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        boolean publicView = !user.isStaff();

        FlightInstanceMealFilter filter = new FlightInstanceMealFilter(
                flightInstanceId, mealId, mealType, dietaryType,
                publicView ? null : isActive, inStock, publicView);

        return ResponseEntity.ok(fimService.getAll(filter, page, size, sortBy, sortDirection));
    }

    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    @PutMapping("/{id}")
    public ResponseEntity<FlightInstanceMealResponse> update(
            CurrentUser user,
            @PathVariable Long id,
            @Valid @RequestBody FlightInstanceMealUpdateRequest request) {
        ownership.flightInstanceMeal(user, id);
        return ResponseEntity.ok(fimService.updateFlightInstanceMeal(id, request));
    }

    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    @PatchMapping("/{id}/status")
    public ResponseEntity<FlightInstanceMealResponse> changeStatus(
            CurrentUser user,
            @PathVariable Long id,
            @RequestParam boolean active) {
        ownership.flightInstanceMeal(user, id);
        return ResponseEntity.ok(fimService.changeStatus(id, active));
    }

    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            CurrentUser user,
            @PathVariable Long id) {
        ownership.flightInstanceMeal(user, id);
        fimService.deleteFlightInstanceMeal(id);
        return ResponseEntity.noContent().build();
    }
}
