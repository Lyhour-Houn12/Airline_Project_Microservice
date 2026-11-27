package com.jing.flight.service;

import com.jing.common.dto.flight.FlightScheduleRequest;
import com.jing.common.dto.flight.GenerateInstancesRequest;
import com.jing.common.dto.flight.FlightScheduleResponse;
import com.jing.common.dto.flight.GenerateInstancesResponse;
import com.jing.common.pagination.PageResponse;

public interface FlightScheduleService {
    FlightScheduleResponse createSchedule(FlightScheduleRequest request);
    PageResponse<FlightScheduleResponse> getSchedules(Long flightId, Boolean isActive, int page, int size);
    FlightScheduleResponse getScheduleById(Long id);
    FlightScheduleResponse updateSchedule(Long id, FlightScheduleRequest request);
    void deleteSchedule(Long id);
    GenerateInstancesResponse generateInstances(Long id, GenerateInstancesRequest request);
}
