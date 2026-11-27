package com.jing.booking.payload.request;

import com.jing.common.enumeration.PassengerSegmentStatus;

public record PassengerSegmentFilter(
        Long segmentId, Long passengerId, PassengerSegmentStatus status,
        Boolean seated          // true = has a seat, false = no seat yet
) {}