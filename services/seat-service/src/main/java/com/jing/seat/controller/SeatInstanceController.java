package com.jing.seat.controller;

import com.jing.common.dto.seat.BlockSeatInstanceRequest;
import com.jing.common.dto.seat.DeleteSeatInstancesResponse;
import com.jing.common.dto.seat.FlightSeatMapResponse;
import com.jing.common.dto.seat.GenerateSeatInstanceRequest;
import com.jing.common.dto.seat.GenerateSeatInstanceResponse;
import com.jing.common.dto.seat.SeatAvailabilitySummary;
import com.jing.common.dto.seat.SeatInstanceResponse;
import com.jing.common.dto.seat.UpdateSurchargeRequest;
import com.jing.common.security.RequireRole;
import com.jing.seat.service.SeatInstanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.jing.common.enumeration.UserRole.ROLE_AIRLINE_OWNER;
import static com.jing.common.enumeration.UserRole.ROLE_SYSTEM_ADMIN;

/** Seat map reads are public; every change is for system admins and airline owners only. */
@RestController
@RequestMapping("/api/v1/seat-instances")
@RequiredArgsConstructor
public class SeatInstanceController {

    private final SeatInstanceService seatInstanceService;

    @PostMapping("/generate")
    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    public ResponseEntity<GenerateSeatInstanceResponse> generate(
            @Valid @RequestBody GenerateSeatInstanceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(seatInstanceService.generate(request));
    }

    @GetMapping("/{id}")
    public SeatInstanceResponse getById(@PathVariable Long id) {
        return seatInstanceService.getById(id);
    }

    @GetMapping("/flight-instance/{flightInstanceId}")
    public FlightSeatMapResponse getSeatMap(@PathVariable Long flightInstanceId) {
        return seatInstanceService.getSeatMap(flightInstanceId);
    }

    @GetMapping("/flight-instance/{flightInstanceId}/summary")
    public SeatAvailabilitySummary getSummary(@PathVariable Long flightInstanceId) {
        return seatInstanceService.getSummary(flightInstanceId);
    }

    @PatchMapping("/{id}/block")
    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    public SeatInstanceResponse block(@PathVariable Long id,
                                      @Valid @RequestBody BlockSeatInstanceRequest request) {
        return seatInstanceService.block(id, request);
    }

    @PatchMapping("/{id}/unblock")
    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    public SeatInstanceResponse unblock(@PathVariable Long id) {
        return seatInstanceService.unblock(id);
    }

    @PatchMapping("/{id}/surcharge")
    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    public SeatInstanceResponse updateSurcharge(@PathVariable Long id,
                                                @Valid @RequestBody UpdateSurchargeRequest request) {
        return seatInstanceService.updateSurcharge(id, request);
    }

    @DeleteMapping("/flight-instance/{flightInstanceId}")
    @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
    public DeleteSeatInstancesResponse deleteByFlightInstance(@PathVariable Long flightInstanceId) {
        return seatInstanceService.deleteByFlightInstance(flightInstanceId);
    }
}
