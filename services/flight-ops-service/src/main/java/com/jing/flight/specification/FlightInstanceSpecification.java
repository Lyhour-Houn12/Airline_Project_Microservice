package com.jing.flight.specification;

import com.jing.common.enumeration.FlightStatus;
import com.jing.flight.filter.FlightInstanceFilter;
import com.jing.flight.model.Flight;
import com.jing.flight.model.FlightInstance;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class FlightInstanceSpecification {
    private FlightInstanceSpecification() {}

    public static Specification<FlightInstance> build(FlightInstanceFilter f, boolean publicView) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();

            if (publicView) {
                // Customers only ever see bookable, upcoming, scheduled instances
                p.add(cb.isTrue(root.get("isActive")));
                p.add(cb.equal(root.get("status"), FlightStatus.SCHEDULED));
                p.add(cb.greaterThan(root.get("departureDateTime"), LocalDateTime.now()));
            }

            if (f != null) {
                // Status filter applies to staff views only; ignored for customers
                if (!publicView && f.getStatus() != null) {
                    p.add(cb.equal(root.get("status"), f.getStatus()));
                }
                if (f.getAirlineId() != null) {
                    p.add(cb.equal(root.get("airlineId"), f.getAirlineId()));
                }
                if (f.getFlightId() != null) {
                    p.add(cb.equal(root.get("flight").get("id"), f.getFlightId()));
                }
                if (f.getDepartureAirportId() != null) {
                    p.add(cb.equal(root.get("departureAirportId"), f.getDepartureAirportId()));
                }
                if (f.getArrivalAirportId() != null) {
                    p.add(cb.equal(root.get("arrivalAirportId"), f.getArrivalAirportId()));
                }
                if (f.getOnDate() != null) {
                    LocalDate day = f.getOnDate();
                    p.add(cb.greaterThanOrEqualTo(root.get("departureDateTime"), day.atStartOfDay()));
                    p.add(cb.lessThan(root.get("departureDateTime"), day.plusDays(1).atStartOfDay()));
                }
                if (f.getKeyword() != null && !f.getKeyword().isBlank()) {
                    Join<FlightInstance, Flight> flight = root.join("flight", JoinType.INNER);
                    String kw = escapeLike(f.getKeyword().trim().toLowerCase());
                    p.add(cb.like(cb.lower(flight.get("flightNumber")), "%" + kw + "%", '\\'));
                }
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}