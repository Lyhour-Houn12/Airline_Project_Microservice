package com.jing.common.dto.seat;

import java.util.List;

/**
 * Result of a confirm. A seat is rejected when its hold was lost (it lapsed and someone else took it);
 * the booking is still confirmed, the passenger just has no seat assigned.
 */
public record SeatConfirmResponse(
        List<Long> confirmed,
        List<Long> rejected
) {
}
