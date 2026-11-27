package com.jing.common.dto.seat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * The booking is paid: its cabin places become booked (internal: booking-service -> seat-service).
 * The counts are sent again, so places whose hold lapsed are booked too: a paid passenger never loses
 * their place, even if that overbooks the cabin (logged).
 */
public record CabinConfirmRequest(
        @NotBlank @Size(max = 20) String holdReference,
        @NotNull List<@Valid Item> cabins
) {
    public record Item(@NotNull Long flightInstanceId, @NotNull Long cabinClassId, @NotNull Integer seats) {}
}
