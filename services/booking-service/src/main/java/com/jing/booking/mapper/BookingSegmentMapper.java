package com.jing.booking.mapper;

import com.jing.booking.model.BookingSegment;
import com.jing.booking.payload.request.BookingSegmentRequest;
import com.jing.booking.payload.response.BookingSegmentResponse;

public final class BookingSegmentMapper {
    private BookingSegmentMapper() {}

    public static BookingSegment toEntity(BookingSegmentRequest r) {
        return BookingSegment.builder()
                .flightId(r.flightId())
                .flightInstanceId(r.flightInstanceId())
                .airlineId(r.airlineId())
                .fareId(r.fareId())
                .cabinClassId(r.cabinClassId())
                .cabinClass(r.cabinClass())
                .departureAirportId(r.departureAirportId())
                .arrivalAirportId(r.arrivalAirportId())
                .departureTime(r.departureTime())
                .arrivalTime(r.arrivalTime())
                .flightNumber(r.flightNumber())
                .build();
    }

    public static BookingSegmentResponse toResponse(BookingSegment s) {
        return new BookingSegmentResponse(
                s.getId(),
                s.getBooking().getId(),
                s.getSegmentOrder(),
                s.getJourneyNumber(),
                s.getFlightId(),
                s.getFlightInstanceId(),
                s.getAirlineId(),
                s.getFareId(),
                s.getCabinClassId(),
                s.getCabinClass(),
                s.getDepartureAirportId(),
                s.getArrivalAirportId(),
                s.getDepartureTime(),
                s.getArrivalTime(),
                s.getFlightNumber(),
                s.getPassengerSegments().stream()
                        .map(PassengerSegmentMapper::toResponse)
                        .toList());
    }
}