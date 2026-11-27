package com.jing.payment.repository;

import com.jing.common.enumeration.PaymentStatus;
import com.jing.payment.model.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTranId(String tranId);

    /** Row lock: refund decisions on one payment happen one at a time. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") Long id);
    Optional<Payment> findByIdempotencyKey(String key);
    List<Payment> findByBookingIdOrderByCreatedAtDesc(Long bookingId);
    List<Payment> findByBookingIdAndStatus(Long bookingId, PaymentStatus status);
    boolean existsByBookingIdAndStatus(Long bookingId, PaymentStatus status);
    List<Payment> findTop100ByStatusAndExpiresAtBefore(PaymentStatus status, Instant time);

    /** PENDING payments old enough that a callback should have arrived (reconciliation job). */
    List<Payment> findTop100ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(PaymentStatus status, Instant time);
}
