package com.jing.booking.service;

import com.jing.booking.model.Booking;
import com.jing.booking.repository.BookingRepository;
import com.jing.common.dto.booking.FlightCancelledRequest;
import com.jing.common.enumeration.BookingStatus;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FlightCancellationServiceTest {

    private final BookingRepository repository = mock(BookingRepository.class);
    private final BookingLifecycle lifecycle = mock(BookingLifecycle.class);
    private final PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);
    private final FlightCancellationService service = new FlightCancellationService(repository, lifecycle, txManager);

    private static Booking booking(long id, BookingStatus status) {
        return Booking.builder().id(id).bookingReference("REF" + id).status(status).currency("USD").build();
    }

    @Test
    void everyActiveBookingOnTheFlightIsCancelledOnce() {
        when(txManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        Booking paid = booking(1, BookingStatus.CONFIRMED);
        Booking unpaid = booking(2, BookingStatus.PENDING);
        Booking cancelledMeanwhile = booking(3, BookingStatus.CANCELLED);   // the customer was faster
        when(repository.findIdsOnFlightInstance(eq(71L), anyCollection())).thenReturn(List.of(1L, 2L, 3L));
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(paid));
        when(repository.findByIdForUpdate(2L)).thenReturn(Optional.of(unpaid));
        when(repository.findByIdForUpdate(3L)).thenReturn(Optional.of(cancelledMeanwhile));

        var result = service.cancelBookingsOn(71L, new FlightCancelledRequest("VN901", "Typhoon"));

        assertThat(result.bookingsCancelled()).isEqualTo(2);
        String reason = "Flight VN901 was cancelled by the airline: Typhoon";
        verify(lifecycle).close(paid, BookingStatus.CANCELLED, "AIRLINE", reason);     // refunded by close()
        verify(lifecycle).close(unpaid, BookingStatus.CANCELLED, "AIRLINE", reason);
        verify(lifecycle, never()).close(eq(cancelledMeanwhile), any(), any(), any());
    }
}
