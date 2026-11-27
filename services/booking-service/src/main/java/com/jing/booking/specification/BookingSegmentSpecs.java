package com.jing.booking.specification;

import com.jing.booking.model.BookingSegment;
import com.jing.booking.payload.request.BookingSegmentFilter;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class BookingSegmentSpecs {
    private BookingSegmentSpecs() {}

    public static Specification<BookingSegment> of(Long bookingId, BookingSegmentFilter f) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("booking").get("id"), bookingId));
            if (f != null) {
                if (f.flightId() != null) p.add(cb.equal(root.get("flightId"), f.flightId()));
                if (f.flightInstanceId() != null) p.add(cb.equal(root.get("flightInstanceId"), f.flightInstanceId()));
                if (f.airlineId() != null) p.add(cb.equal(root.get("airlineId"), f.airlineId()));
                if (f.cabinClass() != null) p.add(cb.equal(root.get("cabinClass"), f.cabinClass()));
                if (f.departureAirportId() != null) p.add(cb.equal(root.get("departureAirportId"), f.departureAirportId()));
                if (f.arrivalAirportId() != null) p.add(cb.equal(root.get("arrivalAirportId"), f.arrivalAirportId()));
                if (f.departureFrom() != null) p.add(cb.greaterThanOrEqualTo(root.get("departureTime"), f.departureFrom()));
                if (f.departureTo() != null) p.add(cb.lessThanOrEqualTo(root.get("departureTime"), f.departureTo()));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }
}