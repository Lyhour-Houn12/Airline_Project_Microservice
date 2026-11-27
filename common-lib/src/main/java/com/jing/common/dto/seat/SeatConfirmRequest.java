package com.jing.common.dto.seat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Turn held seats into booked seats once the booking is paid (internal: booking-service -> seat-service). */
public record SeatConfirmRequest(
        @NotBlank @Size(max = 20) String holdReference,
        @NotEmpty List<Long> seatInstanceIds
) {
}
