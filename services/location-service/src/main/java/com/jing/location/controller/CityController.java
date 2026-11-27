package com.jing.location.controller;

import com.jing.common.dto.location.CityRequest;
import com.jing.common.dto.location.CityResponse;
import com.jing.common.pagination.PageResponse;
import com.jing.common.security.RequireRole;
import com.jing.location.service.CityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.jing.common.enumeration.UserRole.ROLE_SYSTEM_ADMIN;

/** Cities are shared reference data: everyone can read them, only system admins can change them. */
@RestController
@RequestMapping("/api/v1/cities")
@RequiredArgsConstructor
@Validated
public class CityController {

    private final CityService cityService;

    // "/create" is kept so existing clients keep working; prefer POST /api/v1/cities
    @PostMapping({"", "/create"})
    @RequireRole(ROLE_SYSTEM_ADMIN)
    public ResponseEntity<CityResponse> createCity(@Valid @RequestBody CityRequest cityRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cityService.createCity(cityRequest));
    }

    @PostMapping("/bulk")
    @RequireRole(ROLE_SYSTEM_ADMIN)
    public ResponseEntity<List<CityResponse>> createBulkCities(
            @Valid @RequestBody List<CityRequest> requests) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cityService.createBulkCities(requests));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CityResponse> getCityById(@PathVariable Long id) {
        return ResponseEntity.ok(cityService.getCityById(id));
    }

    @GetMapping
    public PageResponse<CityResponse> getAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "cityName") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        return cityService.getAllCities(page, size, sortBy, sortDirection);
    }

    @GetMapping("/search")
    public PageResponse<CityResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "cityName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {
        return cityService.searchCities(keyword, page, size, sortBy, sortDirection);
    }

    @PutMapping("/{id}")
    @RequireRole(ROLE_SYSTEM_ADMIN)
    public ResponseEntity<CityResponse> updateCity(@PathVariable Long id,
                                                   @Valid @RequestBody CityRequest request) {
        return ResponseEntity.ok(cityService.updateCity(id, request));
    }

    @DeleteMapping("/{id}")
    @RequireRole(ROLE_SYSTEM_ADMIN)
    public ResponseEntity<Void> deleteCity(@PathVariable Long id) {
        cityService.deleteCity(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/exists/{cityCode}")
    public ResponseEntity<Boolean> checkCityExists(@PathVariable String cityCode) {
        return ResponseEntity.ok(cityService.cityExists(cityCode.trim().toUpperCase()));
    }
}
