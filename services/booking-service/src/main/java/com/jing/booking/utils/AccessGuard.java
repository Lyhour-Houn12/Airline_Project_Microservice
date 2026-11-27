package com.jing.booking.utils;

import com.jing.booking.model.Booking;
import com.jing.common.exception.ForbiddenException;
import com.jing.common.exception.ResourceNotFoundException;
import com.jing.common.security.CurrentUser;
import com.jing.common.airline.AirlineAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Booking-specific access rules. Who the caller is (and that their role is valid) is already
 * guaranteed by the shared {@link CurrentUser} resolver in common-lib.
 */
@Component
@RequiredArgsConstructor
public class AccessGuard {

    private final AirlineAccess airlineAccess;

    public void requireAdmin(CurrentUser user, String action) {
        if (!user.isAdmin()) {
            throw new ForbiddenException("Only system admins can " + action);
        }
    }

    /** Reads: the booking's owner, a system admin, or the owner of an airline flying one of its segments. */
    public boolean canViewBooking(Booking booking, CurrentUser user) {
        if (user.isAdmin() || isBookingOwner(booking, user)) return true;
        return airlineAccess.ownedAirlineIdIfAny(user)
                .map(airline -> booking.getSegments().stream().anyMatch(s -> airline.equals(s.getAirlineId())))
                .orElse(false);
    }

    /** For searches: the airline whose bookings an airline owner may also see (empty for everyone else). */
    public Optional<Long> visibleAirline(CurrentUser user) {
        return airlineAccess.ownedAirlineIdIfAny(user);
    }

    public boolean isBookingOwner(Booking booking, CurrentUser user) {
        return booking.getUserId().equals(user.id());
    }

    /** Someone else's booking answers 404, exactly like a missing one, so ids can't be probed. */
    public void requireCanView(Booking booking, CurrentUser user) {
        if (!canViewBooking(booking, user)) {
            throw new ResourceNotFoundException("Booking", "id", booking.getId());
        }
    }

    /** Writes: the booking's owner only. */
    public void requireBookingOwner(Booking booking, CurrentUser user) {
        if (!isBookingOwner(booking, user)) {
            throw new ResourceNotFoundException("Booking", "id", booking.getId());
        }
    }
}
