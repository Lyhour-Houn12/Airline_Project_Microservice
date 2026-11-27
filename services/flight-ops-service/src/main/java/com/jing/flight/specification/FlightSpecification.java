package com.jing.flight.specification;

import com.jing.common.enumeration.FlightStatus;
import com.jing.flight.model.Flight;
import org.springframework.data.jpa.domain.Specification;

public final class FlightSpecification {
    private  FlightSpecification() {}

    public static Specification<Flight> hasAirlineId(Long airlineId) {
        return (root, query, cb) -> airlineId == null
                ? cb.conjunction()
                : cb.equal(root.get("airlineId"), airlineId);
    }

    public static Specification<Flight> hasAircraftId(Long aircraftId) {
        return (root, query, cb) -> aircraftId == null
                ? cb.conjunction()
                : cb.equal(root.get("aircraftId"), aircraftId);
    }

    public static Specification<Flight> hasDepartureAirportId(Long departureAirportId) {
        return (root, query, cb) -> departureAirportId == null
                ? cb.conjunction()
                : cb.equal(root.get("departureAirportId"), departureAirportId);
    }

    public static Specification<Flight> hasArrivalAirportId(Long arrivalAirportId) {
        return (root, query, cb) -> arrivalAirportId == null
                ? cb.conjunction()
                : cb.equal(root.get("arrivalAirportId"), arrivalAirportId);
    }

    public static Specification<Flight> hasStatus(FlightStatus status) {
        return (root, query, cb) -> status == null
                ? cb.conjunction()
                : cb.equal(root.get("status"), status);
    }

    public static Specification<Flight> flightNumberContains(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }
            String pattern = "%" + keyword.trim().toUpperCase() + "%";
            return cb.like(cb.upper(root.get("flightNumber")), pattern);
        };
    }

    public static Specification<Flight> withFilters(
                                                    String keyword,
                                                    Long airlineId,
                                                    Long aircraftId,
                                                    Long departureAirportId,
                                                    Long arrivalAirportId,
                                                    FlightStatus status) {
        return Specification
                .where(flightNumberContains(keyword))
                .and(hasAircraftId(aircraftId))
                .and(hasAirlineId(airlineId))
                .and(hasDepartureAirportId(departureAirportId))
                .and(hasArrivalAirportId(arrivalAirportId))
                .and(hasStatus(status));
    }
}
