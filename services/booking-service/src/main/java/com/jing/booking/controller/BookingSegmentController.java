package com.jing.booking.controller;

import com.jing.common.security.CurrentUser;
import com.jing.booking.payload.request.BookingSegmentFilter;
import com.jing.booking.payload.request.BookingSegmentRequest;
import com.jing.booking.payload.response.BookingSegmentResponse;
import com.jing.booking.service.BookingSegmentService;
import com.jing.common.enumeration.CabinClassType;
import com.jing.common.pagination.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
@RestController
@RequestMapping("/api/v1/bookings/{bookingId}/segments")
@RequiredArgsConstructor
public class BookingSegmentController {

    private final BookingSegmentService service;

    @PostMapping
    public ResponseEntity<BookingSegmentResponse> add(@PathVariable Long bookingId,
                                                      @Valid @RequestBody BookingSegmentRequest request,
                                                      CurrentUser user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.addSegment(bookingId, request, user));
    }

    @GetMapping
    public PageResponse<BookingSegmentResponse> list(
            @PathVariable Long bookingId,
            @RequestParam(required = false) Long flightId,
            @RequestParam(required = false) Long flightInstanceId,
            @RequestParam(required = false) Long airlineId,
            @RequestParam(required = false) CabinClassType cabinClass,
            @RequestParam(required = false) Long departureAirportId,
            @RequestParam(required = false) Long arrivalAirportId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime departureFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime departureTo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "segmentOrder") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            CurrentUser user) {

        var filter = new BookingSegmentFilter(flightId, flightInstanceId, airlineId, cabinClass,
                departureAirportId, arrivalAirportId, departureFrom, departureTo);
        return service.getSegments(bookingId, filter, page, size, sortBy, sortDirection, user);

    }
    @GetMapping("/{segmentId}")
    public BookingSegmentResponse get(@PathVariable Long bookingId, @PathVariable Long segmentId,
                                      CurrentUser user) {
        return service.getSegment(bookingId, segmentId, user);
    }
    @DeleteMapping("/{segmentId}")
    public ResponseEntity<Void> delete(@PathVariable Long bookingId, @PathVariable Long segmentId,
                                       CurrentUser user) {
        service.deleteSegment(bookingId, segmentId, user);
        return ResponseEntity.noContent().build();
    }
}