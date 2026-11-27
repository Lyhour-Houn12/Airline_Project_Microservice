package com.jing.payment.service.impl;

import com.jing.common.exception.ServiceCommunicationException;
import com.jing.payment.payload.response.AbaQr;
import org.springframework.web.client.RestClientException;
import java.time.Duration;
import java.util.Map;

import com.jing.common.dto.booking.BookingPaymentInfo;
import com.jing.common.security.CurrentUser;
import com.jing.payment.client.BookingClient;
import java.time.ZoneId;
import java.util.Set;
import com.jing.common.enumeration.PaymentMethod;
import com.jing.common.enumeration.PaymentStatus;
import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.DuplicateResourceException;
import com.jing.common.exception.ResourceNotFoundException;
import com.jing.payment.mapper.PaymentMapper;
import com.jing.payment.model.Payment;
import com.jing.payment.model.PaymentAttempt;
import com.jing.payment.payload.request.AbaCheckout;
import com.jing.payment.payload.request.AbaTxn;
import com.jing.payment.payload.request.CreatePaymentRequest;
import com.jing.payment.payload.request.PaymentStatusChangedEvent;
import com.jing.payment.payload.response.PaymentResponse;
import com.jing.payment.repository.PaymentAttemptRepository;
import com.jing.payment.repository.PaymentRepository;
import com.jing.payment.service.PaymentService;
import com.jing.payment.service.gateway.AbaPayWayClient;
import com.jing.payment.service.gateway.AbaPayWayGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository attemptRepository;
    private final AbaPayWayGateway abaGateway;
    private final AbaPayWayClient abaClient;
    private final ApplicationEventPublisher events;
    private final ObjectMapper mapper;
    private final BookingClient bookingClient;

    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("USD", "KHR");

    @Value("${payment.expiry-minutes:15}")
    private long expiryMinutes;

    @Override
    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest req, CurrentUser user, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                // a key belongs to the user who first used it: never hand someone else's payment back
                if (!existing.get().getUserId().equals(user.id())) {
                    throw new DuplicateResourceException("Payment", "idempotency key", idempotencyKey);
                }
                return PaymentMapper.toResponse(existing.get());
            }
        }
        if (req.method() != PaymentMethod.ABA_PAYWAY && req.method() != PaymentMethod.ABA_PAYWAY_QR) {
            throw new BadRequestException("Supported methods: ABA_PAYWAY (checkout page), ABA_PAYWAY_QR (QR code)");
        }
        // amount, currency and reference come from booking-service (400 if the booking can't be paid yet)
        BookingPaymentInfo booking = bookingClient.paymentInfo(req.bookingId());
        if (!booking.userId().equals(user.id()) && !user.isAdmin()) {
            throw new ResourceNotFoundException("Booking", "id", req.bookingId());   // don't reveal it exists
        }
        if (!SUPPORTED_CURRENCIES.contains(booking.currency())) {
            throw new BadRequestException("ABA PayWay only accepts " + SUPPORTED_CURRENCIES
                    + "; this booking is in " + booking.currency());
        }
        if (req.method() == PaymentMethod.ABA_PAYWAY_QR) {
            AbaPayWayGateway.qrOption(req.paymentOption(), booking.currency());   // 400 before anything is saved
        }
        if (paymentRepository.existsByBookingIdAndStatus(req.bookingId(), PaymentStatus.SUCCESS)) {
            throw new DuplicateResourceException("Booking " + booking.bookingReference() + " is already paid");
        }
        // a new attempt supersedes any open one
        paymentRepository.findByBookingIdAndStatus(req.bookingId(), PaymentStatus.PENDING).forEach(old -> {
            old.setStatus(PaymentStatus.CANCELLED);
            old.setFailureReason("Superseded by a new payment attempt");
        });

        Payment payment = paymentRepository.save(Payment.builder()
                .bookingId(req.bookingId())
                .bookingReference(booking.bookingReference())
                .userId(user.id())
                .tranId(newTranId())
                .amount(booking.totalAmount())
                .currency(booking.currency())
                .method(req.method())
                .status(PaymentStatus.PENDING)
                .expiresAt(paymentDeadline(booking))
                .idempotencyKey(blankToNull(idempotencyKey))
                .build());

        if (req.method() == PaymentMethod.ABA_PAYWAY_QR) {
            return PaymentMapper.toResponse(payment, null, createQr(payment, req));
        }
        AbaCheckout checkout = abaGateway.buildCheckout(payment, req);
        logAttempt(payment.getId(), "CHECKOUT_CREATED", toJson(checkout.fields()));
        return PaymentMapper.toResponse(payment, checkout, null);
    }

    /**
     * Asks PayWay for a QR. Any failure throws, which rolls back the whole createPayment transaction:
     * no half-created payment is left behind and the previous attempt is not superseded.
     */
    private AbaQr createQr(Payment payment, CreatePaymentRequest req) {
        // the QR lives as long as the payment window, at least the 3 minutes PayWay requires
        long seconds = Duration.between(Instant.now(), payment.getExpiresAt()).getSeconds();
        int lifetime = (int) Math.max(3, (seconds + 59) / 60);
        Map<String, String> fields = abaGateway.buildQrFields(payment, req, lifetime);
        try {
            AbaQr qr = abaClient.generateQr(fields, new BigDecimal(fields.get("amount")), lifetime);
            logAttempt(payment.getId(), "QR_CREATED", toJson(fields));
            return qr;
        } catch (AbaPayWayClient.AbaApiException e) {
            log.warn("PayWay refused the QR for payment {}: {} {}", payment.getId(), e.getCode(), e.getMessage());
            throw new BadRequestException("ABA PayWay could not create the QR (" + e.getCode() + "): " + e.getMessage());
        } catch (RestClientException e) {
            throw new ServiceCommunicationException("ABA PayWay is not reachable: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long paymentId, CurrentUser user) {
        return PaymentMapper.toResponse(loadAuthorized(paymentId, user));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByBooking(Long bookingId, CurrentUser user) {
        return paymentRepository.findByBookingIdOrderByCreatedAtDesc(bookingId).stream()
                .filter(p -> user.isAdmin() || p.getUserId().equals(user.id()))
                .map(p -> PaymentMapper.toResponse(p))
                .toList();
    }

    @Override
    public PaymentResponse syncPaymentStatus(Long paymentId, CurrentUser user) {
        Payment p = loadAuthorized(paymentId, user);
        reconcileWithPayWay(p);
        return PaymentMapper.toResponse(p);
    }

    @Override
    @Transactional
    public PaymentResponse cancelPayment(Long paymentId, CurrentUser user) {
        Payment p = loadAuthorized(paymentId, user);
        if (p.getStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException("Only PENDING payments can be cancelled");
        }
        change(p, PaymentStatus.CANCELLED, "Cancelled by user");
        return PaymentMapper.toResponse(p);
    }

    @Override
    @Transactional
    public void processAbaCallback(JsonNode body, String rawBody) {
        String tranId = body.path("tran_id").asText("");
        Payment p = paymentRepository.findByTranId(tranId).orElse(null);
        if (p == null) {
            log.warn("PayWay callback for unknown tran_id {}", tranId);
            return;
        }
        logAttempt(p.getId(), "CALLBACK", rawBody);
        if (p.getStatus() == PaymentStatus.SUCCESS) return;                  // retry, already handled
        if (!"0".equals(body.path("status").asText())) {
            log.warn("PayWay callback for {} has non-success status {}", tranId, body.path("status").asText());
            return;
        }
        boolean amountOk = amountMatches(p, body.path("original_amount").decimalValue())
                && p.getCurrency().equalsIgnoreCase(body.path("original_currency").asText(""));
        if (!amountOk) {
            log.error("Amount/currency mismatch on callback for payment {} - needs manual review", p.getId());
            return;
        }
        try {                                                                  // confirm server-to-server
            AbaTxn txn = abaClient.checkTransaction(tranId);
            logAttempt(p.getId(), "STATUS_CHECK", txn.raw());
            if (!txn.found() || txn.statusCode() != 0) {
                log.warn("Callback says paid but PayWay check says otherwise for {}", tranId);
                return;
            }
        } catch (RuntimeException ex) {
            log.warn("Could not confirm {} with PayWay, trusting the signed callback: {}", tranId, ex.getMessage());
        }
        markSuccess(p, body.path("apv").asText(null), body.path("bank_ref").asText(null),
                body.path("payment_type").asText(null));
    }

    @Override
    public void reconcilePendingPayment(Long paymentId) {
        reconcileWithPayWay(paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId)));
    }



    @Override
    public void reconcileByTranId(String tranId) {
        paymentRepository.findByTranId(tranId).ifPresentOrElse(
                this::reconcileWithPayWay,
                () -> log.warn("PayWay notification for unknown tran_id {}", tranId));
    }

    private void reconcileWithPayWay(Payment p) {
        if (p.getStatus() == PaymentStatus.SUCCESS || p.getStatus() == PaymentStatus.REFUNDED
                || p.getStatus() == PaymentStatus.FAILED) return;
        AbaTxn txn;
        try {
            txn = abaClient.checkTransaction(p.getTranId());
        } catch (RuntimeException ex) {
            log.warn("PayWay status check failed for payment {}: {}", p.getId(), ex.getMessage());
            return;
        }
        logAttempt(p.getId(), "STATUS_CHECK", txn.raw());

        if (!txn.found()) { expireIfDue(p); return; }

        switch (txn.statusCode()) {
            case 0 -> {
                if (amountMatches(p, txn.totalAmount())) markSuccess(p, txn.apv(), null, null);
                else log.error("Amount mismatch on approved payment {} - needs manual review", p.getId());
            }
            case 3 -> { if (p.getStatus() == PaymentStatus.PENDING) change(p, PaymentStatus.FAILED, "Declined by PayWay"); }
            case 7 -> { if (p.getStatus() == PaymentStatus.PENDING) change(p, PaymentStatus.CANCELLED, "Cancelled at PayWay"); }
            case 4 -> {                                  // refunded outside our refund flow (e.g. merchant portal)
                p.setRefundedAmount(p.getAmount());
                change(p, PaymentStatus.REFUNDED, "Refunded at PayWay");
            }
            default -> expireIfDue(p);
        }
    }

    private void markSuccess(Payment p, String apv, String bankRef, String paymentType) {
        if (p.getStatus() == PaymentStatus.SUCCESS) return;
        boolean alreadyPaid = paymentRepository.existsByBookingIdAndStatus(p.getBookingId(), PaymentStatus.SUCCESS);
        p.setStatus(PaymentStatus.SUCCESS);
        p.setPaidAt(Instant.now());
        p.setFailureReason(null);
        p.setProviderRef(apv);
        if (bankRef != null) p.setBankRef(bankRef);
        if (paymentType != null) p.setPaymentType(paymentType);
        if (alreadyPaid) {
            log.error("Booking {} received a second successful payment {} - refund required",
                    p.getBookingReference(), p.getId());
        }
        publish(p);
    }
    private void expireIfDue(Payment p) {
        if (p.getStatus() == PaymentStatus.PENDING && p.getExpiresAt().isBefore(Instant.now())) {
            change(p, PaymentStatus.EXPIRED, "Payment window expired");
        }
    }


    private void change(Payment p, PaymentStatus status, String reason) {
        p.setStatus(status);
        p.setFailureReason(reason);
        publish(p);
    }

    private void publish(Payment p) {
        events.publishEvent(new PaymentStatusChangedEvent(p.getId(), p.getBookingId(),
                p.getBookingReference(), p.getStatus()));
    }

    private Payment loadAuthorized(Long id, CurrentUser user) {
        Payment p = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", id));
        if (!user.isAdmin() && !p.getUserId().equals(user.id())) {
            throw new ResourceNotFoundException("Payment", "id", id);   // don't leak existence
        }
        return p;
    }


    private static boolean amountMatches(Payment p, BigDecimal paid) {
        return paid != null && p.getAmount().compareTo(paid) == 0;
    }

    private static String newTranId() {          // 16 digits, PayWay allows max 20
        return System.currentTimeMillis() + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
    }

    /** The payment window, but never longer than the booking's own hold (its seats are released then). */
    private Instant paymentDeadline(BookingPaymentInfo booking) {
        Instant deadline = Instant.now().plus(expiryMinutes, ChronoUnit.MINUTES);
        if (booking.expiresAt() != null) {
            Instant bookingEnd = booking.expiresAt().atZone(ZoneId.systemDefault()).toInstant();
            if (bookingEnd.isBefore(deadline)) deadline = bookingEnd;
        }
        return deadline;
    }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s; }

    private void logAttempt(Long paymentId, String type, String payload) {
        attemptRepository.save(PaymentAttempt.of(paymentId, type, payload));
    }

    private String toJson(Object o) {
        return mapper.writeValueAsString(o);
    }
}
