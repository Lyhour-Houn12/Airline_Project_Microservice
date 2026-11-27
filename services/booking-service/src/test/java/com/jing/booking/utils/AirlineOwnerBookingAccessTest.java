package com.jing.booking.utils;

import com.jing.booking.model.Booking;
import com.jing.booking.model.BookingSegment;
import com.jing.booking.repository.BookingRepository;
import com.jing.booking.service.BookingLifecycle;
import com.jing.booking.service.BookingProperties;
import com.jing.booking.service.BookingReferenceGenerator;
import com.jing.booking.service.BookingStateMachine;
import com.jing.booking.service.impl.BookingServiceImpl;
import com.jing.common.airline.AirlineAccess;
import com.jing.common.enumeration.BookingStatus;
import com.jing.common.enumeration.TripType;
import com.jing.common.enumeration.UserRole;
import com.jing.common.exception.ForbiddenException;
import com.jing.common.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Airline owners may SEE bookings on their flights, but never change them. */
class AirlineOwnerBookingAccessTest {

    private static final CurrentUser CUSTOMER = new CurrentUser(15L, UserRole.ROLE_CUSTOMER);
    private static final CurrentUser OTHER_CUSTOMER = new CurrentUser(17L, UserRole.ROLE_CUSTOMER);
    private static final CurrentUser OWNER_OF_AIRLINE_3 = new CurrentUser(1L, UserRole.ROLE_AIRLINE_OWNER);
    private static final CurrentUser OWNER_OF_AIRLINE_9 = new CurrentUser(2L, UserRole.ROLE_AIRLINE_OWNER);

    private final AirlineAccess airlineAccess = mock(AirlineAccess.class);
    private final AccessGuard guard = new AccessGuard(airlineAccess);
    private final BookingRepository repository = mock(BookingRepository.class);
    private final BookingLifecycle lifecycle = mock(BookingLifecycle.class);
    private final BookingServiceImpl bookings = new BookingServiceImpl(repository, mock(BookingReferenceGenerator.class),
            new BookingStateMachine(), guard, lifecycle, new BookingProperties(15, 100, 10, 60000));

    private Booking booking;

    @BeforeEach
    void setUp() {
        when(airlineAccess.ownedAirlineIdIfAny(OWNER_OF_AIRLINE_3)).thenReturn(Optional.of(3L));
        when(airlineAccess.ownedAirlineIdIfAny(OWNER_OF_AIRLINE_9)).thenReturn(Optional.of(9L));
        when(airlineAccess.ownedAirlineIdIfAny(CUSTOMER)).thenReturn(Optional.empty());
        when(airlineAccess.ownedAirlineIdIfAny(OTHER_CUSTOMER)).thenReturn(Optional.empty());

        booking = Booking.builder().id(13L).userId(15L).bookingReference("2MVCD4").tripType(TripType.ONE_WAY)
                .status(BookingStatus.PENDING).currency("USD").build();
        booking.addSegment(BookingSegment.builder().id(1L).airlineId(3L).build());   // a flight of airline 3
        when(repository.findById(13L)).thenReturn(Optional.of(booking));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void whoCanSeeTheBooking() {
        assertThat(guard.canViewBooking(booking, CUSTOMER)).isTrue();            // made it
        assertThat(guard.canViewBooking(booking, OWNER_OF_AIRLINE_3)).isTrue();  // flies it
        assertThat(guard.canViewBooking(booking, OWNER_OF_AIRLINE_9)).isFalse(); // other airline
        assertThat(guard.canViewBooking(booking, OTHER_CUSTOMER)).isFalse();
    }

    @Test
    void theAirlineOwnerCannotCancelACustomersBooking() {
        assertThatThrownBy(() -> bookings.cancel(13L, null, OWNER_OF_AIRLINE_3))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(lifecycle);
    }

    @Test
    void theCustomerCanCancelTheirOwnBooking() {
        bookings.cancel(13L, null, CUSTOMER);

        verify(lifecycle).close(booking, BookingStatus.CANCELLED, "15", "Cancelled by user");
    }
}
