package com.jing.flight.controller;

import com.jing.common.dto.flight.FlightScheduleRequest;
import com.jing.common.dto.flight.FlightScheduleResponse;
import com.jing.common.dto.flight.GenerateInstancesRequest;
import com.jing.common.dto.flight.GenerateInstancesResponse;
import com.jing.common.pagination.PageResponse;
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
    public ResponseEntity<FlightScheduleResponse> create(@Valid @RequestBody FlightScheduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.createSchedule(request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<FlightScheduleResponse>> list(
            @RequestParam(required = false) Long flightId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(scheduleService.getSchedules(flightId, isActive, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FlightScheduleResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(scheduleService.getScheduleById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FlightScheduleResponse> update(@PathVariable Long id,
                                                         @Valid @RequestBody FlightScheduleRequest request) {
        return ResponseEntity.ok(scheduleService.updateSchedule(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        scheduleService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/generate-instances")
    public ResponseEntity<GenerateInstancesResponse> generate(@PathVariable Long id,
                                                              @Valid @RequestBody GenerateInstancesRequest request) {
        return ResponseEntity.ok(scheduleService.generateInstances(id, request));
    }
}
