package com.jing.common.dto.seat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Give seats back (internal: booking-service -> seat-service). Releases HELD and BOOKED seats
 * that belong to {@code holdReference}; seats held by anyone else are left alone.
 *
 * @param seatInstanceIds the seats to release; null or empty means every seat of this reference
 */
public record SeatReleaseRequest(
        @NotBlank @Size(max = 20) String holdReference,
        List<Long> seatInstanceIds
) {
}
