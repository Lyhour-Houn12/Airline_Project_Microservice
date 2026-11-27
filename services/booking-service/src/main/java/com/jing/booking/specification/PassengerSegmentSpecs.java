package com.jing.booking.specification;

import com.jing.booking.model.PassengerSegment;
import com.jing.booking.payload.request.PassengerSegmentFilter;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class PassengerSegmentSpecs {
    private PassengerSegmentSpecs() {}

    public static Specification<PassengerSegment> of(Long bookingId, PassengerSegmentFilter f) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("segment").get("booking").get("id"), bookingId));
            if (f != null) {
                if (f.segmentId() != null) p.add(cb.equal(root.get("segment").get("id"), f.segmentId()));
                if (f.passengerId() != null) p.add(cb.equal(root.get("passenger").get("id"), f.passengerId()));
                if (f.status() != null) p.add(cb.equal(root.get("status"), f.status()));
                if (f.seated() != null) {
                    p.add(f.seated()
                            ? cb.isNotNull(root.get("seatInstanceId"))
                            : cb.isNull(root.get("seatInstanceId")));
                }
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }
}