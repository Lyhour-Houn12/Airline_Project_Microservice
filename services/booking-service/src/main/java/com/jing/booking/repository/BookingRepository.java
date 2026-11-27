package com.jing.booking.repository;

import com.jing.booking.model.Booking;
import com.jing.common.enumeration.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {


    boolean existsByBookingReference(String bookingReference);

    Optional<Booking> findByBookingReference(String bookingReference);

    Optional<Booking> findByIdempotencyKey(String idempotencyKey);


    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);

    /** Bookings in the given statuses with a flight on this flight instance (flight cancelled). */
    @Query("""
        SELECT DISTINCT s.booking.id FROM BookingSegment s
        WHERE s.flightInstanceId = :flightInstanceId AND s.booking.status IN :statuses
        """)
    List<Long> findIdsOnFlightInstance(@Param("flightInstanceId") Long flightInstanceId,
                                       @Param("statuses") java.util.Collection<BookingStatus> statuses);

    /** Unpaid bookings whose hold window has passed, oldest first (the expiry job). */
    @Query("SELECT b.id FROM Booking b WHERE b.status = :status AND b.expiresAt < :now ORDER BY b.expiresAt")
    List<Long> findIdsToExpire(@Param("status") BookingStatus status, @Param("now") LocalDateTime now,
                               Pageable pageable);

}
