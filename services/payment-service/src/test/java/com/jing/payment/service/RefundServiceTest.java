package com.jing.payment.service;

import com.jing.common.enumeration.PaymentStatus;
import com.jing.common.exception.BadRequestException;
import com.jing.payment.config.AbaProperties;
import com.jing.payment.model.Payment;
import com.jing.payment.model.PaymentRefund;
import com.jing.payment.payload.request.AbaTxn;
import com.jing.payment.payload.request.PaymentStatusChangedEvent;
import com.jing.payment.payload.response.AbaRefundResult;
import com.jing.payment.payload.response.PaymentRefundResponse;
import com.jing.payment.repository.PaymentAttemptRepository;
import com.jing.payment.repository.PaymentRefundRepository;
import com.jing.payment.repository.PaymentRepository;
import com.jing.payment.service.gateway.AbaPayWayClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RefundServiceTest {

    private final PaymentRepository payments = mock(PaymentRepository.class);
    private final PaymentRefundRepository refunds = mock(PaymentRefundRepository.class);
    private final AbaPayWayClient aba = mock(AbaPayWayClient.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);

    private final Map<Long, PaymentRefund> stored = new HashMap<>();
    private Payment payment;

    private RefundService service(boolean rsaKeyConfigured) {
        AbaProperties props = new AbaProperties("url", "merchant1", "key", null, null,
                rsaKeyConfigured ? "MIIB..." : null, "template3_color");
        return new RefundService(payments, refunds, mock(PaymentAttemptRepository.class), aba, props, events, txManager);
    }

    @BeforeEach
    void setUp() {
        when(txManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        payment = Payment.builder().id(8L).bookingId(7L).bookingReference("R3D8CC").tranId("T1")
                .amount(new BigDecimal("143.00")).currency("USD").status(PaymentStatus.SUCCESS)
                .paidAt(Instant.now()).build();
        when(payments.findById(8L)).thenReturn(Optional.of(payment));
        when(payments.findByIdForUpdate(8L)).thenReturn(Optional.of(payment));

        // a tiny in-memory refund table
        when(refunds.save(any())).thenAnswer(inv -> {
            PaymentRefund r = inv.getArgument(0);
            if (r.getId() == null) r.setId((long) stored.size() + 1);
            stored.put(r.getId(), r);
            return r;
        });
        when(refunds.findById(any())).thenAnswer(inv -> Optional.ofNullable(stored.get(inv.<Long>getArgument(0))));
        when(refunds.sumByStatuses(any(), any())).thenAnswer(inv -> stored.values().stream()
                .filter(r -> r.getStatus() != PaymentRefund.Status.FAILED)
                .map(PaymentRefund::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @Test
    void fullRefundMarksPaymentRefundedAndTellsBooking() {
        when(aba.refund("T1", new BigDecimal("143.00")))
                .thenReturn(new AbaRefundResult("00", "Success", new BigDecimal("143.00"), "REFUNDED", "{}"));

        PaymentRefundResponse r = service(true).refund(8L, null, "Booking cancelled", "BOOKING-SERVICE");

        assertThat(r.status()).isEqualTo(PaymentRefund.Status.SUCCEEDED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.getRefundedAmount()).isEqualByComparingTo("143.00");
        verify(events).publishEvent(new PaymentStatusChangedEvent(8L, 7L, "R3D8CC", PaymentStatus.REFUNDED));
    }

    @Test
    void partialRefundsAddUpAndCannotExceedThePayment() {
        when(aba.refund(any(), any())).thenReturn(new AbaRefundResult("00", "Success", null, "REFUNDED", "{}"));
        RefundService service = service(true);

        service.refund(8L, new BigDecimal("40.00"), "Seat not as described", "ADMIN:14");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
        assertThatThrownBy(() -> service.refund(8L, new BigDecimal("103.01"), "Too much", "ADMIN:14"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("103.00");
        verify(events, never()).publishEvent(any());
    }

    @Test
    void withoutTheRsaKeyARefundWaitsAndPayWayIsNotCalled() {
        PaymentRefundResponse r = service(false).refund(8L, null, "Booking cancelled", "BOOKING-SERVICE");

        assertThat(r.status()).isEqualTo(PaymentRefund.Status.REQUESTED);
        assertThat(r.providerMessage()).contains("ABA_RSA_PUBLIC_KEY");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verifyNoInteractions(aba);
    }

    @Test
    void aTimeoutIsRetriedOnlyAfterCheckingPayWayFirst() {
        RefundService service = service(true);
        when(aba.refund(any(), any())).thenThrow(new RuntimeException("Read timed out"));

        PaymentRefundResponse first = service.refund(8L, null, "Booking cancelled", "BOOKING-SERVICE");
        assertThat(first.status()).isEqualTo(PaymentRefund.Status.REQUESTED);

        // PayWay did process it before the timeout: the retry must NOT refund again
        when(aba.checkTransaction("T1"))
                .thenReturn(new AbaTxn(true, 4, new BigDecimal("143.00"), "apv", new BigDecimal("143.00"), "{}"));
        when(refunds.findTop50ByStatusOrderByCreatedAtAsc(PaymentRefund.Status.REQUESTED))
                .thenReturn(List.of(stored.get(first.id())));

        service.retryWaitingRefunds();

        assertThat(stored.get(first.id()).getStatus()).isEqualTo(PaymentRefund.Status.SUCCEEDED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(aba, times(1)).refund(any(), any());         // only the first, timed-out call
    }

    @Test
    void aFinalPayWayErrorFailsTheRefund() {
        when(aba.refund(any(), any()))
                .thenReturn(new AbaRefundResult("PTL57", "Unable to refund", null, null, "{}"));

        PaymentRefundResponse r = service(true).refund(8L, null, "Booking cancelled", "BOOKING-SERVICE");

        assertThat(r.status()).isEqualTo(PaymentRefund.Status.FAILED);
        assertThat(r.providerCode()).isEqualTo("PTL57");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void onlyWithinThirtyDays() {
        payment.setPaidAt(Instant.now().minus(31, ChronoUnit.DAYS));

        assertThatThrownBy(() -> service(true).refund(8L, null, "Too late", "ADMIN:14"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("30 days");
    }
}
