package com.jing.booking.service;

import com.jing.common.security.CurrentUser;
import com.jing.booking.payload.request.PassengerSegmentFilter;
import com.jing.booking.payload.request.SeatAssignRequest;
import com.jing.booking.payload.response.PassengerSegmentResponse;
import com.jing.common.pagination.PageResponse;


public interface PassengerSegmentService {
    PageResponse<PassengerSegmentResponse> list(Long bookingId, PassengerSegmentFilter filter,
                                                int page, int size, String sortBy, String sortDirection,
                                                CurrentUser user);

    PassengerSegmentResponse get(Long bookingId, Long id, CurrentUser user);
    PassengerSegmentResponse assignSeat(Long bookingId, Long id, SeatAssignRequest request, CurrentUser user);
    PassengerSegmentResponse clearSeat(Long bookingId, Long id, CurrentUser user);
}
