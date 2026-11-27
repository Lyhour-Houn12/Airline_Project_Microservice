package com.jing.booking.mapper;

import com.jing.booking.model.Passenger;
import com.jing.booking.model.PassengerSegment;
import com.jing.booking.payload.response.PassengerSegmentResponse;

public final class PassengerSegmentMapper {
    private PassengerSegmentMapper() {}

    public static PassengerSegmentResponse toResponse(PassengerSegment ps) {
        Passenger p = ps.getPassenger();
        return new PassengerSegmentResponse(
                ps.getId(), ps.getSegment().getId(), p.getId(), p.getFullName(), p.getPassengerType(),
                ps.getSeatInstanceId(), ps.getStatus(),
                ps.getBaseFare(), ps.getTaxesAndFees(), ps.getAirlineFees(), ps.getTotalPrice());
    }
}