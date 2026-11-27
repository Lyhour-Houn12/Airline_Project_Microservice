package com.jing.booking.controller;

import com.jing.common.security.CurrentUser;
import com.jing.booking.payload.request.PassengerRequest;
import com.jing.booking.payload.response.PassengerResponse;
import com.jing.booking.service.PassengerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings/{bookingId}/passengers")
@RequiredArgsConstructor
public class PassengerController {

    private final PassengerService passengerService;

    @PostMapping
    public ResponseEntity<PassengerResponse> add(
            @PathVariable Long bookingId,
            @Valid @RequestBody PassengerRequest request,
            CurrentUser user) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(passengerService.add(bookingId, request, user));
    }

    @GetMapping
    public ResponseEntity<List<PassengerResponse>> getAll(
            @PathVariable Long bookingId,
            CurrentUser user) {

        return ResponseEntity.ok(passengerService.getAll(bookingId, user));
    }

    @GetMapping("/{passengerId}")
    public ResponseEntity<PassengerResponse> getById(
            @PathVariable Long bookingId,
            @PathVariable Long passengerId,
            CurrentUser user) {

        return ResponseEntity.ok(passengerService.getById(bookingId, passengerId, user));
    }

    @PutMapping("/{passengerId}")
    public ResponseEntity<PassengerResponse> update(
            @PathVariable Long bookingId,
            @PathVariable Long passengerId,
            @Valid @RequestBody PassengerRequest request,
            CurrentUser user) {

        return ResponseEntity.ok(
                passengerService.update(bookingId, passengerId, request, user));
    }

    @DeleteMapping("/{passengerId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long bookingId,
            @PathVariable Long passengerId,
            CurrentUser user) {

        passengerService.delete(bookingId, passengerId, user);
        return ResponseEntity.noContent().build();
    }
}