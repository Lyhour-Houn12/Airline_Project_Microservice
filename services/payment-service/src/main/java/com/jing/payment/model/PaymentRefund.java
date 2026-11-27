package com.jing.payment.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One refund of (part of) a payment. Every refund is kept, also failed ones, so there is a full trail.
 *
 * <pre>
 *   REQUESTED --PayWay "00"--> SUCCEEDED
 *   REQUESTED --PayWay error-> FAILED      (a final error: amount too high, not refundable...)
 *   REQUESTED stays REQUESTED              (temporary error, or no RSA key yet: the retry job tries again)
 * </pre>
 */
@Entity
@Table(name = "payment_refunds", indexes = {
        @Index(name = "idx_refund_payment", columnList = "paymentId"),
        @Index(name = "idx_refund_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRefund {

    public enum Status { REQUESTED, SUCCEEDED, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private Long paymentId;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 3) private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.REQUESTED;

    @Column(length = 255) private String reason;
    @Column(length = 50) private String requestedBy;        // "ADMIN:14", "BOOKING-CANCELLED", ...
    @Column(length = 20) private String providerCode;       // PayWay status.code, e.g. "00", "PTL181"
    @Column(length = 255) private String providerMessage;
    @Builder.Default
    private int attempts = 0;

    @Version private Long version;
    @CreationTimestamp private Instant createdAt;
    @UpdateTimestamp private Instant updatedAt;
}
