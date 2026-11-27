package com.jing.seat.service;

import com.jing.common.dto.seat.BlockSeatInstanceRequest;
import com.jing.common.dto.seat.DeleteSeatInstancesResponse;
import com.jing.common.dto.seat.GenerateSeatInstanceRequest;
import com.jing.common.dto.seat.UpdateSurchargeRequest;
import com.jing.common.dto.seat.FlightSeatMapResponse;
import com.jing.common.dto.seat.GenerateSeatInstanceResponse;
import com.jing.common.dto.seat.SeatAvailabilitySummary;
import com.jing.common.dto.seat.SeatInstanceResponse;

public interface SeatInstanceService {

    GenerateSeatInstanceResponse generate(GenerateSeatInstanceRequest request);

    FlightSeatMapResponse getSeatMap(Long flightInstanceId);

    SeatAvailabilitySummary getSummary(Long flightInstanceId);

    SeatInstanceResponse getById(Long id);

    SeatInstanceResponse block(Long id, BlockSeatInstanceRequest request);

    SeatInstanceResponse unblock(Long id);

    SeatInstanceResponse updateSurcharge(Long id, UpdateSurchargeRequest request);

    DeleteSeatInstancesResponse deleteByFlightInstance(Long flightInstanceId);
}
