package com.jing.ancillary.controller;

import com.jing.common.security.RequireRole;
import com.jing.common.security.AuthUser;
import com.jing.common.security.CurrentUser;
import com.jing.ancillary.filter.MealFilter;
import com.jing.ancillary.service.MealService;
import com.jing.common.enumeration.DietaryType;
import com.jing.common.enumeration.MealType;
import com.jing.common.dto.ancillary.MealRequest;
import com.jing.common.dto.ancillary.MealResponse;
import com.jing.common.pagination.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

import static com.jing.common.enumeration.UserRole.ROLE_AIRLINE_OWNER;
import static com.jing.common.enumeration.UserRole.ROLE_SYSTEM_ADMIN;

@RestController
@RequestMapping("/api/v1/meals")
@RequiredArgsConstructor
public class MealController {

    private final MealService mealService;

    // 1. CREATE meal: multipart = "meal" (JSON) + optional "file" (image)
    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MealResponse> create(
            @Valid @RequestPart("meal") MealRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mealService.createMeal(request, file));
    }

    // 2. READ
    @GetMapping("/{id}")
    public ResponseEntity<MealResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(mealService.getById(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<MealResponse>> getAll(
            @AuthUser(required = false) CurrentUser user,
            @RequestParam(required = false) Long airlineId,
            @RequestParam(required = false) MealType mealType,
            @RequestParam(required = false) DietaryType dietaryType,
            @RequestParam(required = false) String cuisine,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer maxCalories,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        // customers / anonymous callers only see active meals
        Boolean activeFilter = user.isStaff() ? isActive : Boolean.TRUE;

        MealFilter filter = new MealFilter(airlineId, mealType, dietaryType, cuisine,
                currency, activeFilter, keyword, maxCalories, minPrice, maxPrice);

        return ResponseEntity.ok(mealService.getAll(filter, page, size, sortBy, sortDirection));
    }

    // 3. EDIT text fields only (JSON), the image is not touched
    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MealResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody MealRequest request) {
        return ResponseEntity.ok(mealService.updateMeal(id, request));
    }

    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    @PatchMapping("/{id}/status")
    public ResponseEntity<MealResponse> changeStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {
        return ResponseEntity.ok(mealService.changeStatus(id, active));
    }

    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id) {
        mealService.deleteMeal(id);
        return ResponseEntity.noContent().build();
    }

    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MealResponse> uploadImage(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(mealService.uploadImage(id, file));
    }

    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    @DeleteMapping("/{id}/image")
    public ResponseEntity<MealResponse> deleteImage(
            @PathVariable Long id) {
        return ResponseEntity.ok(mealService.deleteImage(id));
    }
}
