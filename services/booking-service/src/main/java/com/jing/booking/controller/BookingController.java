package com.jing.booking.controller;


import com.jing.common.security.CurrentUser;
import com.jing.booking.payload.request.BookingFilter;
import com.jing.booking.payload.request.CancelBookingRequest;
import com.jing.booking.payload.request.ChangeStatusRequest;
import com.jing.booking.payload.request.CreateDraftBookingRequest;
import com.jing.booking.payload.response.BookingResponse;
import com.jing.booking.payload.response.BookingStatusHistoryResponse;
import com.jing.booking.service.BookingService;
import com.jing.booking.utils.AccessGuard;
import com.jing.common.enumeration.BookingStatus;
import com.jing.common.pagination.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    // Step 1 of booking: an empty PENDING booking. Then add flights, passengers, seats, and pay.
    @PostMapping("/draft")
    public ResponseEntity<BookingResponse> createDraft(
            @Valid @RequestBody CreateDraftBookingRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            CurrentUser user) {

        BookingResponse response = bookingService.createDraft(
                request, user, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<BookingResponse>> search(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) String bookingReference,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "bookingDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            CurrentUser user) {

        BookingFilter filter = new BookingFilter(
                userId, status, bookingReference, createdFrom, createdTo);

        return ResponseEntity.ok(bookingService.search(
                filter, user,
                page, size, sortBy, sortDirection));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getById(
            @PathVariable Long id,
            CurrentUser user) {

        return ResponseEntity.ok(bookingService.getById(id, user));
    }

    @GetMapping("/reference/{bookingReference}")
    public ResponseEntity<BookingResponse> getByReference(
            @PathVariable String bookingReference,
            CurrentUser user) {

        return ResponseEntity.ok(bookingService.getByReference(
                bookingReference, user));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<BookingStatusHistoryResponse>> getHistory(
            @PathVariable Long id,
            CurrentUser user) {

        return ResponseEntity.ok(bookingService.getHistory(id, user));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancel(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) CancelBookingRequest request,
            CurrentUser user) {

        return ResponseEntity.ok(bookingService.cancel(id, request, user));
    }

    // admin only: the service enforces it through AccessGuard
    @PatchMapping("/{id}/status")
    public ResponseEntity<BookingResponse> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeStatusRequest request,
            CurrentUser user) {

        return ResponseEntity.ok(bookingService.changeStatus(
                id, request, user));
    }
}
