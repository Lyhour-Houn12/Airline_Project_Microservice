package com.jing.booking.service;

import com.jing.booking.model.Booking;
import com.jing.booking.repository.BookingRepository;
import com.jing.common.enumeration.BookingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Expires unpaid bookings once their hold window (booking.hold-minutes) has passed, and gives their
 * seats back. Each booking is expired in its own transaction, so one failure doesn't stop the rest.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingExpiryJob {

    private static final int BATCH_SIZE = 100;

    private final BookingRepository bookingRepository;
    private final BookingLifecycle lifecycle;
    private final PlatformTransactionManager transactionManager;

    @Scheduled(fixedDelayString = "${booking.expiry-check-millis:60000}")
    public void expireUnpaidBookings() {
        List<Long> due = bookingRepository.findIdsToExpire(BookingStatus.PENDING, LocalDateTime.now(),
                PageRequest.of(0, BATCH_SIZE));
        if (due.isEmpty()) return;

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        int expired = 0;
        for (Long id : due) {
            try {
                Boolean done = tx.execute(status -> expire(id));
                if (Boolean.TRUE.equals(done)) expired++;
            } catch (ObjectOptimisticLockingFailureException e) {
                log.info("Booking {} changed while expiring it (probably just paid); skipped", id);
            } catch (RuntimeException e) {
                log.error("Could not expire booking {}", id, e);
            }
        }
        log.info("Expired {} unpaid booking(s)", expired);
    }

    private boolean expire(Long id) {
        Booking booking = bookingRepository.findByIdForUpdate(id).orElse(null);
        // re-check inside the transaction: it may have been paid or cancelled in the meantime
        if (booking == null || !booking.isExpired(LocalDateTime.now())) return false;
        lifecycle.close(booking, BookingStatus.EXPIRED, "SYSTEM", "Not paid in time");
        return true;
    }
}
