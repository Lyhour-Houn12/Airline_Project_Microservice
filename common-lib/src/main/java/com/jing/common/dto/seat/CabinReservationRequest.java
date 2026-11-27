package com.jing.common.dto.seat;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Hold places in a cabin for a booking (internal: booking-service -> seat-service).
 * It SETS the booking's places in that cabin (not adds), so sending it again is harmless;
 * 0 gives them all back.
 *
 * @param seats passengers who need a seat (adults and children; infants sit on a lap)
 */
public record CabinReservationRequest(
        @NotNull Long flightInstanceId,
        @NotNull Long cabinClassId,
        @NotBlank @Size(max = 20) String holdReference,
        @NotNull @Min(0) @Max(9) Integer seats,
        @NotNull @Future LocalDateTime holdUntil
) {
}
