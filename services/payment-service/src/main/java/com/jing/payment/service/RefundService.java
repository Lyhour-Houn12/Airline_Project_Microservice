package com.jing.payment.service;

import com.jing.common.enumeration.PaymentStatus;
import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.ResourceNotFoundException;
import com.jing.payment.config.AbaProperties;
import com.jing.payment.model.Payment;
import com.jing.payment.model.PaymentAttempt;
import com.jing.payment.model.PaymentRefund;
import com.jing.payment.payload.request.AbaTxn;
import com.jing.payment.payload.request.PaymentStatusChangedEvent;
import com.jing.payment.payload.response.AbaRefundResult;
import com.jing.payment.payload.response.PaymentRefundResponse;
import com.jing.payment.repository.PaymentAttemptRepository;
import com.jing.payment.repository.PaymentRefundRepository;
import com.jing.payment.repository.PaymentRepository;
import com.jing.payment.service.gateway.AbaPayWayClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Refunds through PayWay's Refund API.
 *
 * <p><b>Never refund twice.</b> Calling PayWay can't be part of our database transaction, so:
 * <ol>
 *   <li>the attempt is counted and committed BEFORE PayWay is called;</li>
 *   <li>PayWay is called outside any transaction;</li>
 *   <li>the answer is saved in a new transaction.</li>
 * </ol>
 * If we crash or time out between 1 and 3, the refund stays REQUESTED with attempts > 0, and the next
 * try first asks check-transaction how much PayWay already refunded instead of sending it again.
 *
 * <p>PayWay's rules: only completed payments, within 30 days, partial refunds allowed up to the
 * amount paid. Without ABA_RSA_PUBLIC_KEY refunds are recorded and wait for the retry job.
 */
@Slf4j
@Service
public class RefundService {

    private static final Duration REFUND_WINDOW = Duration.ofDays(30);
    private static final Set<PaymentStatus> REFUNDABLE = EnumSet.of(PaymentStatus.SUCCESS, PaymentStatus.PARTIALLY_REFUNDED);
    private static final Set<PaymentRefund.Status> COMMITTED =
            EnumSet.of(PaymentRefund.Status.REQUESTED, PaymentRefund.Status.SUCCEEDED);

    private final PaymentRepository paymentRepository;
    private final PaymentRefundRepository refundRepository;
    private final PaymentAttemptRepository attemptRepository;
    private final AbaPayWayClient abaClient;
    private final AbaProperties props;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate tx;          // always a NEW transaction (also safe after-commit)

    public RefundService(PaymentRepository paymentRepository, PaymentRefundRepository refundRepository,
                         PaymentAttemptRepository attemptRepository, AbaPayWayClient abaClient,
                         AbaProperties props, ApplicationEventPublisher events,
                         PlatformTransactionManager transactionManager) {
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.attemptRepository = attemptRepository;
        this.abaClient = abaClient;
        this.props = props;
        this.events = events;
        this.tx = new TransactionTemplate(transactionManager);
        this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Record a refund and try it right away.
     *
     * @param amount null refunds everything that hasn't been refunded (or requested) yet
     */
    public PaymentRefundResponse refund(Long paymentId, BigDecimal amount, String reason, String requestedBy) {
        Long refundId = tx.execute(status -> create(paymentId, amount, reason, requestedBy).getId());
        process(refundId);
        return PaymentRefundResponse.from(refundRepository.findById(refundId).orElseThrow());
    }

    /** Refund what is left of a booking's payment; nothing to do if it was never paid or is already refunded. */
    public Optional<PaymentRefundResponse> refundBooking(Long bookingId, String reason, String requestedBy) {
        Optional<Payment> paid = paymentRepository.findByBookingIdOrderByCreatedAtDesc(bookingId).stream()
                .filter(p -> REFUNDABLE.contains(p.getStatus()))
                .findFirst();
        if (paid.isEmpty()) {
            log.info("Booking {} has no successful payment to refund", bookingId);
            return Optional.empty();
        }
        Payment p = paid.get();
        if (remaining(p).signum() <= 0) {
            return Optional.empty();   // already fully refunded or requested: a repeated call is harmless
        }
        return Optional.of(refund(p.getId(), null, reason, requestedBy));
    }

    public List<PaymentRefundResponse> list(Long paymentId) {
        return refundRepository.findByPaymentIdOrderByCreatedAtDesc(paymentId).stream()
                .map(PaymentRefundResponse::from).toList();
    }

    /** Retries refunds that are still waiting (no RSA key yet, PayWay busy, network error...). */
    @Scheduled(fixedDelayString = "${payment.refund-retry-millis:300000}",
               initialDelayString = "${payment.refund-retry-millis:300000}")
    public void retryWaitingRefunds() {
        if (!props.refundsConfigured()) return;
        for (PaymentRefund r : refundRepository.findTop50ByStatusOrderByCreatedAtAsc(PaymentRefund.Status.REQUESTED)) {
            try {
                process(r.getId());
            } catch (RuntimeException e) {
                log.warn("Refund {} retry failed: {}", r.getId(), e.getMessage());
            }
        }
    }

    // ------------------------------------------------------------------ steps

    private PaymentRefund create(Long paymentId, BigDecimal amount, String reason, String requestedBy) {
        Payment p = paymentRepository.findByIdForUpdate(paymentId)   // one refund decision at a time
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));
        if (!REFUNDABLE.contains(p.getStatus())) {
            throw new BadRequestException("Payment " + paymentId + " is " + p.getStatus() + " and cannot be refunded");
        }
        Instant paidOn = p.getPaidAt() != null ? p.getPaidAt() : p.getCreatedAt();
        if (paidOn != null && paidOn.plus(REFUND_WINDOW).isBefore(Instant.now())) {
            throw new BadRequestException("PayWay only refunds within 30 days of the payment");
        }
        BigDecimal remaining = remaining(p);
        BigDecimal value = amount != null ? amount : remaining;
        if (value.signum() <= 0) {
            throw new BadRequestException("Nothing left to refund on payment " + paymentId);
        }
        if (value.compareTo(remaining) > 0) {
            throw new BadRequestException("At most " + remaining + " " + p.getCurrency() + " can still be refunded");
        }
        if ("KHR".equals(p.getCurrency()) && value.stripTrailingZeros().scale() > 0) {
            throw new BadRequestException("KHR refunds must be whole riel");
        }
        return refundRepository.save(PaymentRefund.builder()
                .paymentId(p.getId())
                .amount(value)
                .currency(p.getCurrency())
                .reason(reason)
                .requestedBy(requestedBy)
                .build());
    }

    void process(Long refundId) {
        Attempt attempt = tx.execute(status -> startAttempt(refundId));
        if (attempt == null) return;                         // nothing to do (done, or waiting for the key)

        if (attempt.retry()) {
            // an earlier attempt may have reached PayWay: look before refunding again
            AbaTxn txn;
            try {
                txn = abaClient.checkTransaction(attempt.tranId());
            } catch (RuntimeException e) {
                tx.executeWithoutResult(s -> note(refundId, null, "Check before retry failed: " + e.getMessage()));
                return;
            }
            if (txn.found() && txn.refundAmount() != null
                    && txn.refundAmount().compareTo(attempt.refundedBefore().add(attempt.amount())) >= 0) {
                tx.executeWithoutResult(s -> succeed(refundId, "00", "Already refunded at PayWay (verified)", null));
                return;
            }
        }

        AbaRefundResult result;
        try {
            result = abaClient.refund(attempt.tranId(), attempt.amount());
        } catch (RuntimeException e) {
            // timeout, network, 5xx: unknown outcome. Stays REQUESTED; the retry checks PayWay first.
            log.warn("Refund {} call to PayWay failed: {}", refundId, e.getMessage());
            tx.executeWithoutResult(s -> note(refundId, null, "PayWay call failed: " + e.getMessage()));
            return;
        }

        AbaRefundResult r = result;
        tx.executeWithoutResult(s -> {
            if (r.succeeded()) {
                succeed(refundId, r.code(), r.message(), r.raw());
            } else if (r.temporary()) {
                note(refundId, r.code(), r.message());
            } else {
                fail(refundId, r);
            }
        });
    }

    private record Attempt(String tranId, BigDecimal amount, BigDecimal refundedBefore, boolean retry) {}

    private Attempt startAttempt(Long refundId) {
        PaymentRefund refund = refundRepository.findById(refundId).orElseThrow();
        if (refund.getStatus() != PaymentRefund.Status.REQUESTED) return null;
        if (!props.refundsConfigured()) {
            refund.setProviderMessage("Waiting: ABA_RSA_PUBLIC_KEY is not configured");
            return null;
        }
        Payment p = paymentRepository.findById(refund.getPaymentId()).orElseThrow();
        boolean retry = refund.getAttempts() > 0;
        refund.setAttempts(refund.getAttempts() + 1);       // committed before PayWay is called
        return new Attempt(p.getTranId(), refund.getAmount(), p.getRefundedAmount(), retry);
    }

    private void succeed(Long refundId, String code, String message, String raw) {
        PaymentRefund refund = refundRepository.findById(refundId).orElseThrow();
        if (refund.getStatus() != PaymentRefund.Status.REQUESTED) return;
        refund.setStatus(PaymentRefund.Status.SUCCEEDED);
        refund.setProviderCode(code);
        refund.setProviderMessage(message);

        Payment p = paymentRepository.findByIdForUpdate(refund.getPaymentId()).orElseThrow();
        p.setRefundedAmount(p.getRefundedAmount().add(refund.getAmount()));
        boolean full = p.getRefundedAmount().compareTo(p.getAmount()) >= 0;
        p.setStatus(full ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED);
        if (raw != null) attemptRepository.save(PaymentAttempt.of(p.getId(), "REFUND", raw));
        log.info("Refund {} of {} {} for payment {} succeeded ({})",
                refundId, refund.getAmount(), refund.getCurrency(), p.getId(), p.getStatus());
        if (full) {
            // tells booking-service after commit (BookingNotificationListener)
            events.publishEvent(new PaymentStatusChangedEvent(p.getId(), p.getBookingId(),
                    p.getBookingReference(), PaymentStatus.REFUNDED));
        }
    }

    private void fail(Long refundId, AbaRefundResult r) {
        PaymentRefund refund = refundRepository.findById(refundId).orElseThrow();
        refund.setStatus(PaymentRefund.Status.FAILED);
        refund.setProviderCode(r.code());
        refund.setProviderMessage(r.message());
        attemptRepository.save(PaymentAttempt.of(refund.getPaymentId(), "REFUND_FAILED", r.raw()));
        log.error("Refund {} rejected by PayWay: {} {}", refundId, r.code(), r.message());
    }

    private void note(Long refundId, String code, String message) {
        refundRepository.findById(refundId).ifPresent(refund -> {
            refund.setProviderCode(code);
            refund.setProviderMessage(message == null ? null : message.substring(0, Math.min(255, message.length())));
        });
    }

    /** Paid minus everything refunded or still being refunded. */
    private BigDecimal remaining(Payment p) {
        return p.getAmount().subtract(refundRepository.sumByStatuses(p.getId(), COMMITTED));
    }
}
