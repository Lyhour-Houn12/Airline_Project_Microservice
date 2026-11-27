package com.jing.booking.specification;

import com.jing.booking.model.Booking;
import com.jing.common.security.CurrentUser;
import com.jing.booking.payload.request.BookingFilter;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class BookingSpecification {

    private BookingSpecification() {}

    public static Specification<Booking> withFilter(BookingFilter filter, CurrentUser user){
        return ((root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();

            BookingFilter f = filter != null
                    ? filter
                    : new BookingFilter(null, null, null, null, null);

            // customers (and, for now, airline owners) can only see their own bookings,
            // whatever userId they send
            boolean admin = user.isAdmin();
            Long effectiveUserId = admin ? f.userId() : user.id();

            if (effectiveUserId != null) p.add(cb.equal(root.get("userId"), effectiveUserId));
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
