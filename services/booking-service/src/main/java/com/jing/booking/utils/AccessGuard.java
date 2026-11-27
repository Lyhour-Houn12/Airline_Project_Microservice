package com.jing.booking.utils;

import com.jing.booking.model.Booking;
import com.jing.common.exception.ForbiddenException;
import com.jing.common.exception.ResourceNotFoundException;
import com.jing.common.security.CurrentUser;
import org.springframework.stereotype.Component;

/**
 * Booking-specific access rules. Who the caller is (and that their role is valid) is already
 * guaranteed by the shared {@link CurrentUser} resolver in common-lib.
 */
@Component
public class AccessGuard {

    public void requireAdmin(CurrentUser user, String action) {
        if (!user.isAdmin()) {
            throw new ForbiddenException("Only system admins can " + action);
        }
    }

    /** Reads: the booking's owner or a system admin. */
    public boolean canViewBooking(Booking booking, CurrentUser user) {
        if (user.isAdmin()) return true;
        // later: an airline owner of one of the booking's segments may also view it
        return isBookingOwner(booking, user);
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
