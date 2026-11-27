package com.jing.booking.service;

import com.jing.common.security.CurrentUser;
import com.jing.booking.payload.request.PassengerRequest;
import com.jing.booking.payload.response.PassengerResponse;

import java.util.List;


public interface PassengerService {

    PassengerResponse add(Long bookingId, PassengerRequest request, CurrentUser user);
    List<PassengerResponse> getAll(Long bookingId, CurrentUser user);
    PassengerResponse getById(Long bookingId, Long passengerId, CurrentUser user);
    PassengerResponse update(Long bookingId, Long passengerId, PassengerRequest request, CurrentUser user);
    void delete(Long bookingId, Long passengerId, CurrentUser user);
}
