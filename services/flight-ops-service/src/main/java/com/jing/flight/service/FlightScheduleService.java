package com.jing.flight.service;

import com.jing.common.security.CurrentUser;
import com.jing.common.dto.flight.FlightScheduleRequest;
import com.jing.common.dto.flight.GenerateInstancesRequest;
import com.jing.common.dto.flight.FlightScheduleResponse;
import com.jing.common.dto.flight.GenerateInstancesResponse;
import com.jing.common.pagination.PageResponse;

public interface FlightScheduleService {
    FlightScheduleResponse createSchedule(CurrentUser user, FlightScheduleRequest request);
    PageResponse<FlightScheduleResponse> getSchedules(CurrentUser user, Long flightId, Boolean isActive, int page, int size);
    FlightScheduleResponse getScheduleById(CurrentUser user, Long id);
    FlightScheduleResponse updateSchedule(CurrentUser user, Long id, FlightScheduleRequest request);
    void deleteSchedule(CurrentUser user, Long id);
    GenerateInstancesResponse generateInstances(CurrentUser user, Long id, GenerateInstancesRequest request);
}
