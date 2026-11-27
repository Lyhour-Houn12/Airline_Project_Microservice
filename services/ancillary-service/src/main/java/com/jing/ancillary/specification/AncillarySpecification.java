package com.jing.ancillary.specification;

import com.jing.ancillary.filter.AncillaryFilter;
import com.jing.ancillary.model.Ancillary;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class AncillarySpecification {

    private AncillarySpecification() {
    }
    public static Specification<Ancillary> withFilter(AncillaryFilter f) {
        return ((root, query, cb) ->{
            List<Predicate> p = new ArrayList<>();

            if (f.airlineId() != null) {
                p.add(cb.equal(root.get("airlineId"), f.airlineId()));
            }
            if (f.type() != null) {
                p.add(cb.equal(root.get("type"), f.type()));
            }
            if (f.currency() != null && !f.currency().isBlank()) {
                p.add(cb.equal(root.get("currency"), f.currency().trim().toUpperCase()));
            }
            if (f.isActive() != null) {
                p.add(cb.equal(root.get("isActive"), f.isActive()));
            }
            if (f.keyword() != null && !f.keyword().isBlank()) {
                String like = "%" + f.keyword().trim().toLowerCase() + "%";
                p.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("code")), like)));
            }
            if (f.minPrice() != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("basePrice"), f.minPrice()));
            }
            if (f.maxPrice() != null) {
                p.add(cb.lessThanOrEqualTo(root.get("basePrice"), f.maxPrice()));
            }
            return cb.and(p.toArray(new Predicate[0]));
        });
    }

}