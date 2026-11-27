package com.jing.flight.controller;

import com.jing.common.dto.flight.FlightInstanceRequest;
import com.jing.common.dto.flight.FlightInstanceResponse;
import com.jing.common.enumeration.FlightStatus;
import com.jing.common.pagination.PageResponse;
import com.jing.common.airline.AirlineAccess;
import com.jing.common.security.AuthUser;
import com.jing.common.security.CurrentUser;
import com.jing.common.security.RequireRole;
import com.jing.flight.filter.FlightInstanceFilter;
import com.jing.flight.service.FlightInstanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.jing.common.enumeration.UserRole.ROLE_AIRLINE_OWNER;
import static com.jing.common.enumeration.UserRole.ROLE_SYSTEM_ADMIN;

@RestController
@RequestMapping("/api/v1/flight-instances")
@RequiredArgsConstructor
public class FlightInstanceController {

    private final FlightInstanceService flightInstanceService;
    private final AirlineAccess airlineAccess;

    @PostMapping
    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    public ResponseEntity<FlightInstanceResponse> create(CurrentUser user,
                                                         @Valid @RequestBody FlightInstanceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(flightInstanceService.createFlightInstanceWithCabins(user, request));
    }

    /** Public: admins and airline owners get the staff view, everyone else the customer view. */
    @GetMapping
    public ResponseEntity<PageResponse<FlightInstanceResponse>> list(
            @AuthUser(required = false) CurrentUser user,
            @ModelAttribute FlightInstanceFilter filter,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        boolean publicView = !user.isStaff();
        if (user.isAirlineOwner()) {
            filter.setAirlineId(airlineAccess.ownedAirlineId(user));   // staff view of their own airline only
        }
        return ResponseEntity.ok(flightInstanceService
                .getFlightInstances(filter, publicView, page, size, sortBy, sortDirection));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FlightInstanceResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(flightInstanceService.getFlightInstanceById(id));
    }

    /** Internal (service-to-service): fetch many instances at once, /batch?ids=1,2,3 */
    @GetMapping("/batch")
    public ResponseEntity<Map<Long, FlightInstanceResponse>> getByIds(@RequestParam List<Long> ids) {
        return ResponseEntity.ok(flightInstanceService.getFlightInstancesByIds(ids));
    }

    @PutMapping("/{id}")
    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    public ResponseEntity<FlightInstanceResponse> update(CurrentUser user, @PathVariable Long id,
                                                         @Valid @RequestBody FlightInstanceRequest request) {
        return ResponseEntity.ok(flightInstanceService.updateFlightInstance(user, id, request));
    }

    @PatchMapping("/{id}/status")
    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    public ResponseEntity<FlightInstanceResponse> changeStatus(CurrentUser user, @PathVariable Long id,
                                                               @RequestParam FlightStatus status,
                                                               @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(flightInstanceService.changeStatus(user, id, status, reason));
    }

    @DeleteMapping("/{id}")
    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    public ResponseEntity<Void> delete(CurrentUser user, @PathVariable Long id) {
        flightInstanceService.deleteFlightInstance(user, id);
        return ResponseEntity.noContent().build();
    }
}
