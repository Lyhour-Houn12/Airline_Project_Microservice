package com.jing.seat.service;

import com.jing.common.dto.seat.SeatConfirmRequest;
import com.jing.common.dto.seat.SeatConfirmResponse;
import com.jing.common.dto.seat.SeatHoldRequest;
import com.jing.common.dto.seat.SeatInstanceResponse;
import com.jing.common.dto.seat.SeatReleaseRequest;

/**
 * Seat inventory for bookings: AVAILABLE -> HELD (while the customer pays) -> BOOKED,
 * or back to AVAILABLE when the booking is cancelled, expires or picks another seat.
 */
public interface SeatHoldService {

    SeatInstanceResponse hold(Long seatInstanceId, SeatHoldRequest request);

    SeatConfirmResponse confirm(SeatConfirmRequest request);

    /** @return how many seats were released */
    int release(SeatReleaseRequest request);
}
