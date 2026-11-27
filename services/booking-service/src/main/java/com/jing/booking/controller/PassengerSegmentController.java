package com.jing.booking.controller;

import com.jing.common.security.CurrentUser;
import com.jing.booking.payload.request.SeatAssignRequest;
import com.jing.booking.payload.response.PassengerSegmentResponse;
import com.jing.booking.service.PassengerSegmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings/{bookingId}/passenger-segments")
@RequiredArgsConstructor
public class PassengerSegmentController {

    private final PassengerSegmentService service;


    @GetMapping("/{id}")
    public PassengerSegmentResponse get(@PathVariable Long bookingId, @PathVariable Long id,
                                        CurrentUser user) {
        return service.get(bookingId, id, user);
    }

    @PutMapping("/{id}/seat")
    public PassengerSegmentResponse assignSeat(@PathVariable Long bookingId, @PathVariable Long id,
                                               @Valid @RequestBody SeatAssignRequest request,
                                               CurrentUser user) {
        return service.assignSeat(bookingId, id, request, user);
    }

    @DeleteMapping("/{id}/seat")
    public PassengerSegmentResponse clearSeat(@PathVariable Long bookingId, @PathVariable Long id,
                                              CurrentUser user) {
        return service.clearSeat(bookingId, id, user);
    }
}
