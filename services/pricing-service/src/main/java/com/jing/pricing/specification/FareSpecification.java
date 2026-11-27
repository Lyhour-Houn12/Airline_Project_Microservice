package com.jing.pricing.specification;

import com.jing.pricing.model.Fare;
import com.jing.pricing.payload.filter.FareFilter;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class FareSpecification {

    private FareSpecification() {}

    public static Expression<BigDecimal> effectivePrice(Root<Fare> root, CriteriaBuilder cb){
        Expression<BigDecimal> total = cb.sum(
                cb.sum(
                        root.<BigDecimal>get("baseFare"), cb.coalesce(root.<BigDecimal>get("taxesAndFees"), BigDecimal.ZERO)),
                cb.coalesce(root.<BigDecimal>get("airlineFees"), BigDecimal.ZERO)
        );
        return cb.coalesce(root.<BigDecimal>get("currentPrice"), total);
    }

    public static Specification<Fare> withFilter(FareFilter f){
        return ((root, query, cb) ->{
            if (f == null) return cb.conjunction();

            List<Predicate> predicates = new ArrayList<>();
            if (f.airlineId() != null)    predicates.add(cb.equal(root.get("airlineId"), f.airlineId()));
            if (f.flightId() != null)     predicates.add(cb.equal(root.get("flightId"), f.flightId()));
            if (f.cabinClassId() != null) predicates.add(cb.equal(root.get("cabinClassId"), f.cabinClassId()));
            if (f.cabinClass() != null)   predicates.add(cb.equal(root.get("cabinClass"), f.cabinClass()));

            if (StringUtils.hasText(f.currency())) {
                predicates.add(cb.equal(cb.upper(root.get("currency")), f.currency().trim().toUpperCase()));
            }
            if (f.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(effectivePrice(root, cb), f.minPrice()));
            }
            if (f.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(effectivePrice(root, cb), f.maxPrice()));
            }


            return cb.and(predicates.toArray(new Predicate[0]));
        } );
    }

    public static Specification<Fare> orderByEffectivePrice(boolean ascending){
        return (root, query, cb) ->{
            Class<?> resultType = query.getResultType();
            if (resultType != Long.class && resultType != long.class){
                Expression<BigDecimal> price = effectivePrice(root, cb);
                query.orderBy(ascending ? cb.asc(price) : cb.desc(price));
            }

            return cb.conjunction();
        };
    }
}
