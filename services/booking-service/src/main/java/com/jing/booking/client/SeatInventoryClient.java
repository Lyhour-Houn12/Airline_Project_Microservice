package com.jing.booking.client;

import com.jing.common.dto.seat.CabinConfirmRequest;
import com.jing.common.dto.seat.CabinReservationRequest;
import com.jing.common.dto.seat.CabinReservationResponse;
import com.jing.common.dto.seat.SeatConfirmRequest;
import com.jing.common.dto.seat.SeatConfirmResponse;
import com.jing.common.dto.seat.SeatHoldRequest;
import com.jing.common.dto.seat.SeatInstanceResponse;
import com.jing.common.dto.seat.SeatReleaseRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/** seat-service's internal inventory API (not reachable through the gateway). */
@FeignClient(name = "seat-service", contextId = "seatInventoryClient")
public interface SeatInventoryClient {

    @PostMapping("/internal/v1/seat-instances/{id}/hold")
    SeatInstanceResponse hold(@PathVariable("id") Long seatInstanceId, @RequestBody SeatHoldRequest request);

    @PostMapping("/internal/v1/seat-instances/confirm")
    SeatConfirmResponse confirm(@RequestBody SeatConfirmRequest request);

    @PostMapping("/internal/v1/seat-instances/release")
    Map<String, Integer> release(@RequestBody SeatReleaseRequest request);

    @PostMapping("/internal/v1/seat-instances/cabin-reservations")
    CabinReservationResponse reserveCabin(@RequestBody CabinReservationRequest request);

    @PostMapping("/internal/v1/seat-instances/cabin-reservations/confirm")
    void confirmCabins(@RequestBody CabinConfirmRequest request);
}
