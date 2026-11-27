package com.jing.ancillary.specification;

import com.jing.ancillary.filter.FlightCabinAncillaryFilter;
import com.jing.ancillary.model.Ancillary;
import com.jing.ancillary.model.FlightCabinAncillary;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class FlightCabinAncillarySpecification {

    private FlightCabinAncillarySpecification() {}

    public static Specification<FlightCabinAncillary> withFilter(FlightCabinAncillaryFilter f) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            Join<FlightCabinAncillary, Ancillary> ancillary = root.join("ancillary");

            if (f.flightInstanceCabinId() != null) {
                p.add(cb.equal(root.get("flightInstanceCabinId"), f.flightInstanceCabinId()));
            }
            if (f.ancillaryId() != null) {
                p.add(cb.equal(ancillary.get("id"), f.ancillaryId()));
            }
            if (f.ancillaryType() != null) {
                p.add(cb.equal(ancillary.get("type"), f.ancillaryType()));
            }
            if (f.isFree() != null) {
                p.add(cb.equal(root.get("isFree"), f.isFree()));
            }
            if (f.isActive() != null) {
                p.add(cb.equal(root.get("isActive"), f.isActive()));
            }
            if (Boolean.TRUE.equals(f.inStock())) {
                p.add(cb.or(
                        cb.isNull(root.get("totalQuantity")),
                        cb.greaterThan(
                                cb.diff(root.<Integer>get("totalQuantity"), root.<Integer>get("soldQuantity")), 0)));
            }
            if (f.publicView()) {
                p.add(cb.isTrue(root.get("isActive")));
                p.add(cb.isTrue(ancillary.get("isActive")));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }
}