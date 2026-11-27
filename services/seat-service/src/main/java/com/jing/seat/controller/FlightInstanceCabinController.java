package com.jing.seat.controller;

import com.jing.common.dto.seat.FlightInstanceCabinBulkRequest;
import com.jing.common.dto.seat.FlightInstanceCabinResponse;
import com.jing.common.dto.seat.FlightInstanceCabinUpdateRequest;
import com.jing.common.enumeration.CabinClassType;
import com.jing.common.pagination.PageResponse;
import com.jing.common.security.RequireRole;
import com.jing.seat.filter.FlightInstanceCabinFilter;
import com.jing.seat.service.FlightInstanceCabinService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.jing.common.enumeration.UserRole.ROLE_AIRLINE_OWNER;
import static com.jing.common.enumeration.UserRole.ROLE_SYSTEM_ADMIN;

@RestController
@RequestMapping("/api/v1/flight-instance-cabins")
@RequiredArgsConstructor
public class FlightInstanceCabinController {

    private final FlightInstanceCabinService cabinService;

    @PostMapping
    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    public ResponseEntity<List<FlightInstanceCabinResponse>> create(
            @Valid @RequestBody FlightInstanceCabinBulkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cabinService.createCabins(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FlightInstanceCabinResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(cabinService.getById(id));
    }

    @GetMapping("/flight-instance/{flightInstanceId}")
    public ResponseEntity<List<FlightInstanceCabinResponse>> getByFlightInstance(@PathVariable Long flightInstanceId) {
        return ResponseEntity.ok(cabinService.getByFlightInstanceId(flightInstanceId));
    }

    @GetMapping
    public ResponseEntity<PageResponse<FlightInstanceCabinResponse>> search(
            @RequestParam(required = false) Long flightInstanceId,
            @RequestParam(required = false) Long cabinClassId,
            @RequestParam(required = false) Long aircraftId,
            @RequestParam(required = false) CabinClassType cabinClassType,
            @RequestParam(required = false) Boolean isBookable,
            @RequestParam(required = false) Boolean hasAvailability,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {
        FlightInstanceCabinFilter filter = new FlightInstanceCabinFilter(
                flightInstanceId, cabinClassId, aircraftId, cabinClassType, isBookable, hasAvailability);
        return ResponseEntity.ok(cabinService.search(filter, page, size, sortBy, sortDirection));
    }

    @PutMapping("/{id}")
    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    public ResponseEntity<FlightInstanceCabinResponse> update(@PathVariable Long id,
                                                              @Valid @RequestBody FlightInstanceCabinUpdateRequest request) {
        return ResponseEntity.ok(cabinService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        cabinService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
