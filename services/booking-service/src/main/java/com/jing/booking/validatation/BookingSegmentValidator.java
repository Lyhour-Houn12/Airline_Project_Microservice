package com.jing.booking.validatation;

import com.jing.booking.model.Booking;
import com.jing.booking.model.BookingSegment;
import com.jing.booking.payload.request.BookingSegmentRequest;
import com.jing.common.enumeration.TripType;
import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.DuplicateResourceException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class BookingSegmentValidator {

    private static final int MAX_SEGMENTS = 9;

    /** Validates a new segment and returns the journey number it belongs to. */
    public int validate(Booking booking, BookingSegmentRequest req){
        List<BookingSegment> existing = booking.getSegments();
        TripType type = booking.getTripType();

        if (existing.size() >= MAX_SEGMENTS)
            throw new BadRequestException("A booking can have at most " + MAX_SEGMENTS + " segments");

        // --- the segment on its own ---
        if (req.departureAirportId().equals(req.arrivalAirportId()))
            throw new BadRequestException("Departure and arrival airport must be different");
        if (!req.arrivalTime().isAfter(req.departureTime()))
            throw new BadRequestException("Arrival time must be after departure time");
        if (!req.departureTime().isAfter(LocalDateTime.now()))
            throw new BadRequestException("Departure time must be in the future");

        if (existing.stream().anyMatch(s -> s.getFlightInstanceId().equals(req.flightInstanceId())))
            throw new DuplicateResourceException("The flight", "id", req.flightInstanceId());

        // --- where it sits in the booking ---
        BookingSegment last = existing.isEmpty() ? null : existing.get(existing.size() - 1);
        int journey = resolveJourney(type, last, req.journeyNumber());

        if (last != null) {
            if (!req.departureTime().isAfter(last.getArrivalTime()))
                throw new BadRequestException("Segment must depart after the previous segment arrives");

            if (journey == last.getJourneyNumber()) {
                if (!req.departureAirportId().equals(last.getArrivalAirportId()))
                    throw new BadRequestException("A connecting segment must depart from the airport where the "
                            + "previous one landed (airport " + last.getArrivalAirportId() + ")");
            } else if (type == TripType.ROUND_TRIP
                    && !req.departureAirportId().equals(last.getArrivalAirportId())) {
                // OPEN-JAW: delete this branch to allow returning from a different airport
                throw new BadRequestException("The return journey must start from the airport where the "
                        + "outbound journey ended (airport " + last.getArrivalAirportId() + ")");
            }
        }

        // a journey must not come back to where it started (that would be a round trip)
        existing.stream().filter(s -> s.getJourneyNumber() == journey).findFirst().ifPresent(first -> {
            if (req.arrivalAirportId().equals(first.getDepartureAirportId()))
                throw new BadRequestException("A journey cannot return to its starting airport. "
                        + "Use a round trip with a second journey instead");
        });

        return journey;
    }



    public void validateComplete(Booking booking){
        List<BookingSegment> segs = booking.getSegments();

        if (segs.isEmpty())
            throw new BadRequestException("The booking has no flight");

        BookingSegment first = segs.get(0);
        BookingSegment last = segs.get(segs.size() - 1);
        int journeys = last.getJourneyNumber();

        switch (booking.getTripType()){
            case ROUND_TRIP -> {
                if (journeys != 2){
                    throw new BadRequestException("A round trip needs an outbound and a return journey");
                }

                if (!last.getArrivalAirportId().equals(first.getDepartureAirportId()))
                    throw new BadRequestException("The return journey must end at the outbound origin");
            }
            case MULTI_CITY -> {
                if (journeys < 2)
                    throw new BadRequestException("A multi-city booking needs at least 2 journeys");
            }
            default -> { }

        }
    }

    /** After a delete, the remaining segments must still connect. */
    public void validateAfterRemoval(Booking booking, List<BookingSegment> remaining){
        for (int i = 1; i < remaining.size(); i++){
            BookingSegment prev = remaining.get(i - 1);
            BookingSegment cur = remaining.get(i);

            boolean mustConnect = prev.getJourneyNumber() == cur.getJourneyNumber()
                    || booking.getTripType() == TripType.ROUND_TRIP;

            if (mustConnect && !cur.getDepartureAirportId().equals(prev.getArrivalAirportId()))
                throw new BadRequestException("Removing this segment would break the connection between "
                        + label(prev) + " and " + label(cur) + ". Remove the later segments first");
        }
    }



    private int resolveJourney(TripType type, BookingSegment last, Integer requested){
        int min = last == null ? 1 : last.getJourneyNumber();
        int max = last == null ? 1 : last.getJourneyNumber() + 1;   // same journey or the next one
        int journey = requested != null ? requested : min;
        if (journey < min || journey > max)
            throw new BadRequestException(last == null
                    ? "The first segment must be in journey 1"
                    : "journeyNumber must be " + min + " (same journey) or " + max + " (next journey)");
        int maxJourneys = switch (type) {
            case ONE_WAY -> 1;
            case ROUND_TRIP -> 2;
            default -> MAX_SEGMENTS;
        };

        if (journey > maxJourneys)
            throw new BadRequestException(type + " booking can have at most " + maxJourneys + " journey(s)");
        return journey;

    }


    private String label(BookingSegment s) {
        return s.getFlightNumber() != null ? s.getFlightNumber() : "segment " + s.getSegmentOrder();
    }
}
