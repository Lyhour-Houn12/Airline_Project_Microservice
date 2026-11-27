package com.jing.payment.repository;

import com.jing.common.enumeration.PaymentStatus;
import com.jing.payment.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTranId(String tranId);
    Optional<Payment> findByIdempotencyKey(String key);
    List<Payment> findByBookingIdOrderByCreatedAtDesc(Long bookingId);
    List<Payment> findByBookingIdAndStatus(Long bookingId, PaymentStatus status);
    boolean existsByBookingIdAndStatus(Long bookingId, PaymentStatus status);
    List<Payment> findTop100ByStatusAndExpiresAtBefore(PaymentStatus status, Instant time);
}
