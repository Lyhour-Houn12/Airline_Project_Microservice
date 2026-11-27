package com.jing.booking.specification;

import com.jing.booking.model.Booking;
import com.jing.booking.model.BookingSegment;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import com.jing.common.security.CurrentUser;
import com.jing.booking.payload.request.BookingFilter;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class BookingSpecification {

    private BookingSpecification() {}

    public static Specification<Booking> withFilter(BookingFilter filter, CurrentUser user, Long visibleAirlineId){
        return ((root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();

            BookingFilter f = filter != null
                    ? filter
                    : new BookingFilter(null, null, null, null, null);

            // admins see everything (optionally one user's); customers only their own bookings,
            // whatever userId they send; airline owners their own plus those with a flight on their airline
            if (user.isAdmin()) {
                if (f.userId() != null) p.add(cb.equal(root.get("userId"), f.userId()));
            } else if (visibleAirlineId != null) {
                Subquery<Long> onOurFlights = query.subquery(Long.class);
                Root<BookingSegment> segment = onOurFlights.from(BookingSegment.class);
                onOurFlights.select(segment.get("booking").get("id"))
                        .where(cb.equal(segment.get("airlineId"), visibleAirlineId));
                p.add(cb.or(cb.equal(root.get("userId"), user.id()), root.get("id").in(onOurFlights)));
            } else {
                p.add(cb.equal(root.get("userId"), user.id()));
            }
            if (f.status() != null) p.add(cb.equal(root.get("status"), f.status()));
            if (f.bookingReference() != null && !f.bookingReference().isBlank()) {
                p.add(cb.equal(root.get("bookingReference"), f.bookingReference().trim().toUpperCase()));
            }
            if (f.createdFrom() != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("bookingDate"), f.createdFrom().atStartOfDay()));
            }
            if (f.createdTo() != null) {
                p.add(cb.lessThan(root.get("bookingDate"), f.createdTo().plusDays(1).atStartOfDay()));
            }
            return cb.and(p.toArray(new Predicate[0]));

        });
    }

}
