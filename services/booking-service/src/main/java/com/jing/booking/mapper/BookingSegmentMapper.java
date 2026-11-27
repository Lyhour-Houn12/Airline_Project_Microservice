package com.jing.booking.mapper;

import com.jing.booking.model.BookingSegment;
import com.jing.common.dto.flight.FlightInstanceResponse;
import com.jing.common.dto.pricing.FareResponse;
import com.jing.booking.payload.response.BookingSegmentResponse;

public final class BookingSegmentMapper {
    private BookingSegmentMapper() {}

    /** Builds a segment from the real flight and fare (journey, order and prices are set by the service). */
    public static BookingSegment toEntity(FlightInstanceResponse flight, FareResponse fare) {
        return BookingSegment.builder()
                .flightId(flight.getFlightId())
                .flightInstanceId(flight.getId())
                .airlineId(flight.getAirlineId())
                .fareId(fare.getId())
                .cabinClassId(fare.getCabinClassId())
                .cabinClass(fare.getCabinClass())
                .departureAirportId(flight.getDepartureAirportId())
                .arrivalAirportId(flight.getArrivalAirportId())
                .departureTime(flight.getDepartureDateTime())
                .arrivalTime(flight.getArrivalDateTime())
                .departureAt(flight.getDepartureAt())
                .arrivalAt(flight.getArrivalAt())
                .departureTimeZone(flight.getDepartureTimeZone())
                .arrivalTimeZone(flight.getArrivalTimeZone())
                .flightNumber(flight.getFlightNumber())
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
                s.getDepartureAt(),
                s.getArrivalAt(),
                s.getDepartureTimeZone(),
                s.getArrivalTimeZone(),
                s.getFlightNumber(),
                s.getPassengerSegments().stream()
                        .map(PassengerSegmentMapper::toResponse)
                        .toList());
    }
}