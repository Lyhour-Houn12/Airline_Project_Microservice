package com.jing.seat.specification;

import com.jing.seat.model.SeatMap;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class SeatMapSpecification {

    private SeatMapSpecification() {
    }
    public static Specification<SeatMap> filter(String keyword, Long cabinId, Long airlineId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%" + keyword.trim().toLowerCase() + "%"));}

            if (cabinId != null) {predicates.add(criteriaBuilder.equal(root.get("cabinClass").get("id"), cabinId));}
            if (airlineId != null) {predicates.add(criteriaBuilder.equal(root.get("airlineId"), airlineId));}

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}