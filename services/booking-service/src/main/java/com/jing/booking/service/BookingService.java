package com.jing.booking.service;

import com.jing.common.security.CurrentUser;
import com.jing.booking.payload.request.BookingFilter;
import com.jing.booking.payload.request.CancelBookingRequest;
import com.jing.booking.payload.request.ChangeStatusRequest;
import com.jing.booking.payload.request.CreateDraftBookingRequest;
import com.jing.booking.payload.response.BookingResponse;
import com.jing.booking.payload.response.BookingStatusHistoryResponse;
import com.jing.common.pagination.PageResponse;

import java.util.List;

public interface BookingService {


    BookingResponse createDraft(CreateDraftBookingRequest request, CurrentUser user, String idempotencyKey);

    BookingResponse getById(Long id, CurrentUser user);

    BookingResponse getByReference(String bookingReference, CurrentUser user);

    PageResponse<BookingResponse> search(BookingFilter filter, CurrentUser user,  int page, int size, String sortBy, String sortDirection);


    List<BookingStatusHistoryResponse> getHistory(Long id, CurrentUser user);

    BookingResponse cancel(Long id, CancelBookingRequest request, CurrentUser user);

    // admin-only for now; confirm/expire will be called internally later
    BookingResponse changeStatus(Long id, ChangeStatusRequest request, CurrentUser user);
}
