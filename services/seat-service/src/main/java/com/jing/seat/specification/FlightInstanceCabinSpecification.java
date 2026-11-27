package com.jing.seat.specification;

import com.jing.seat.filter.FlightInstanceCabinFilter;
import com.jing.seat.model.FlightInstanceCabin;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class FlightInstanceCabinSpecification {

    private FlightInstanceCabinSpecification() {}

    public static Specification<FlightInstanceCabin> toSpec(FlightInstanceCabinFilter f) {
        return (root, query, cb) -> {
            if (f == null) return cb.conjunction();
            List<Predicate> p = new ArrayList<>();
            if (f.flightInstanceId() != null) p.add(cb.equal(root.get("flightInstanceId"), f.flightInstanceId()));
            if (f.cabinClassId() != null) p.add(cb.equal(root.get("cabinClass").get("id"), f.cabinClassId()));
            if (f.aircraftId() != null) p.add(cb.equal(root.get("cabinClass").get("aircraftId"), f.aircraftId()));
            if (f.cabinClassType() != null)
                p.add(cb.equal(root.get("cabinClass").get("cabinClassType"), f.cabinClassType()));
            if (f.isBookable() != null) p.add(cb.equal(root.get("isBookable"), f.isBookable()));
            if (f.hasAvailability() != null) {
                var available = cb.diff(root.<Integer>get("totalSeats"), root.<Integer>get("bookedSeats"));
                p.add(f.hasAvailability() ? cb.greaterThan(available, 0) : cb.lessThanOrEqualTo(available, 0));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }
}
