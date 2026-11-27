package com.jing.flight.controller;

import com.jing.common.dto.flight.FlightScheduleRequest;
import com.jing.common.dto.flight.FlightScheduleResponse;
import com.jing.common.dto.flight.GenerateInstancesRequest;
import com.jing.common.dto.flight.GenerateInstancesResponse;
import com.jing.common.pagination.PageResponse;
import com.jing.common.security.CurrentUser;
import com.jing.common.security.RequireRole;
import com.jing.flight.service.FlightScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.jing.common.enumeration.UserRole.ROLE_AIRLINE_OWNER;
import static com.jing.common.enumeration.UserRole.ROLE_SYSTEM_ADMIN;

/** Schedules are a staff tool: every endpoint needs a system admin or an airline owner. */
@RestController
@RequestMapping("/api/v1/flight-schedules")
@RequiredArgsConstructor
@RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
public class FlightScheduleController {

    private final FlightScheduleService scheduleService;

    @PostMapping
    public ResponseEntity<FlightScheduleResponse> create(CurrentUser user, @Valid @RequestBody FlightScheduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.createSchedule(user, request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<FlightScheduleResponse>> list(
            CurrentUser user,
            @RequestParam(required = false) Long flightId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(scheduleService.getSchedules(user, flightId, isActive, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FlightScheduleResponse> getById(CurrentUser user, @PathVariable Long id) {
        return ResponseEntity.ok(scheduleService.getScheduleById(user, id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FlightScheduleResponse> update(CurrentUser user, @PathVariable Long id,
                                                         @Valid @RequestBody FlightScheduleRequest request) {
        return ResponseEntity.ok(scheduleService.updateSchedule(user, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(CurrentUser user, @PathVariable Long id) {
        scheduleService.deleteSchedule(user, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/generate-instances")
    public ResponseEntity<GenerateInstancesResponse> generate(CurrentUser user, @PathVariable Long id,
                                                              @Valid @RequestBody GenerateInstancesRequest request) {
        return ResponseEntity.ok(scheduleService.generateInstances(user, id, request));
    }
}
