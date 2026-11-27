package com.jing.ancillary.specification;

import com.jing.ancillary.filter.InsuranceCoverageFilter;
import com.jing.ancillary.model.InsuranceCoverage;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class InsuranceCoverageSpecification {

    private InsuranceCoverageSpecification() {}

    public static Specification<InsuranceCoverage> withFilter(InsuranceCoverageFilter f) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();

            if (f.airlineId() != null) {
                p.add(cb.equal(root.get("airlineId"), f.airlineId()));
            }
            if (f.coverageType() != null) {
                p.add(cb.equal(root.get("coverageType"), f.coverageType()));
            }
            if (f.provider() != null && !f.provider().isBlank()) {
                p.add(cb.like(cb.lower(root.get("provider")),
                        "%" + f.provider().trim().toLowerCase() + "%"));
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
            if (f.eligibleAge() != null) {
                // a null limit means "no limit"
                p.add(cb.or(cb.isNull(root.get("minAge")),
                        cb.lessThanOrEqualTo(root.get("minAge"), f.eligibleAge())));
                p.add(cb.or(cb.isNull(root.get("maxAge")),
                        cb.greaterThanOrEqualTo(root.get("maxAge"), f.eligibleAge())));
            }
            if (f.minPremium() != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("premium"), f.minPremium()));
            }
            if (f.maxPremium() != null) {
                p.add(cb.lessThanOrEqualTo(root.get("premium"), f.maxPremium()));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }
}