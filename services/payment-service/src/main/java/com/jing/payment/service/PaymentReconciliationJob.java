package com.jing.payment.service;

import com.jing.common.enumeration.PaymentStatus;
import com.jing.payment.model.Payment;
import com.jing.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Safety net for missed callbacks. Every minute, PENDING payments older than a minute are checked
 * with PayWay's check-transaction API: paid ones become SUCCESS (and confirm their booking), the
 * rest become EXPIRED once their window has passed. Each payment is handled in its own transaction.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentReconciliationJob {

    private static final Duration GRACE = Duration.ofMinutes(1);   // give the callback a chance first

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    @Scheduled(fixedDelayString = "${payment.reconcile-every-millis:60000}",
               initialDelayString = "${payment.reconcile-every-millis:60000}")
    public void reconcilePendingPayments() {
        List<Payment> pending = paymentRepository.findTop100ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
                PaymentStatus.PENDING, Instant.now().minus(GRACE));
        for (Payment p : pending) {
            try {
                paymentService.reconcilePendingPayment(p.getId());   // own transaction (service proxy)
            } catch (RuntimeException e) {
                log.warn("Could not reconcile payment {}: {}", p.getId(), e.getMessage());
            }
        }
        if (!pending.isEmpty()) {
            log.info("Reconciled {} pending payment(s) with PayWay", pending.size());
        }
    }
}
