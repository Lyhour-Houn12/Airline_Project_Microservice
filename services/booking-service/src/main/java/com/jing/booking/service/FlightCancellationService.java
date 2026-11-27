package com.jing.booking.service;

import com.jing.booking.model.Booking;
import com.jing.booking.repository.BookingRepository;
import com.jing.common.dto.booking.FlightCancelledRequest;
import com.jing.common.dto.booking.FlightCancelledResponse;
import com.jing.common.enumeration.BookingStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The airline cancelled a flight: every active booking on it is cancelled, in its own transaction.
 * {@link BookingLifecycle#close} does the rest: seats and cabin places released, tickets voided, and a
 * FULL refund requested for paid bookings (the airline cancelled, so fare rules don't apply).
 * Safe to repeat: bookings that are already cancelled are skipped.
 */
@Slf4j
@Service
public class FlightCancellationService {

    private static final Set<BookingStatus> ACTIVE = EnumSet.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);

    private final BookingRepository bookingRepository;
    private final BookingLifecycle lifecycle;
    private final TransactionTemplate tx;

    public FlightCancellationService(BookingRepository bookingRepository, BookingLifecycle lifecycle,
                                     PlatformTransactionManager transactionManager) {
        this.bookingRepository = bookingRepository;
        this.lifecycle = lifecycle;
        this.tx = new TransactionTemplate(transactionManager);
    }

    public FlightCancelledResponse cancelBookingsOn(Long flightInstanceId, FlightCancelledRequest request) {
        List<Long> ids = bookingRepository.findIdsOnFlightInstance(flightInstanceId, ACTIVE);
        String reason = "Flight " + (request.flightNumber() != null ? request.flightNumber() : flightInstanceId)
                + " was cancelled by the airline" + (request.reason() != null ? ": " + request.reason() : "");
        int cancelled = 0;
        for (Long id : ids) {
            Boolean done = tx.execute(status -> {
                Booking booking = bookingRepository.findByIdForUpdate(id).orElse(null);
                if (booking == null || !ACTIVE.contains(booking.getStatus())) return false;   // changed meanwhile
                lifecycle.close(booking, BookingStatus.CANCELLED, "AIRLINE", reason);
                return true;
            });
            if (Boolean.TRUE.equals(done)) cancelled++;
        }
        log.info("{}: {} booking(s) cancelled", reason, cancelled);
        return new FlightCancelledResponse(flightInstanceId, cancelled);
    }
}
