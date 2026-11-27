package com.jing.booking.service;

import com.jing.common.security.CurrentUser;
import com.jing.booking.payload.request.BookingSegmentFilter;
import com.jing.booking.payload.request.BookingSegmentRequest;
import com.jing.booking.payload.response.BookingSegmentResponse;
import com.jing.common.pagination.PageResponse;

public interface BookingSegmentService {

    BookingSegmentResponse addSegment(Long bookingId, BookingSegmentRequest request, CurrentUser user);

    PageResponse<BookingSegmentResponse> getSegments(Long bookingId, BookingSegmentFilter filter,
                                                     int page, int size, String sortBy, String sortDirection,
                                                     CurrentUser user);

    BookingSegmentResponse getSegment(Long bookingId, Long segmentId, CurrentUser user);

    void deleteSegment(Long bookingId, Long segmentId, CurrentUser user);
}
