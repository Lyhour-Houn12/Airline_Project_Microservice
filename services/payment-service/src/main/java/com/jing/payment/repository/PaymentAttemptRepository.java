package com.jing.payment.repository;

import com.jing.payment.model.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {
}
