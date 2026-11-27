package com.jing.payment.model;

import com.jing.common.enumeration.PaymentMethod;
import com.jing.common.enumeration.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments", indexes = @Index(name = "idx_payment_booking", columnList = "bookingId"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false) private Long bookingId;
    @Column(nullable = false, length = 50) private String bookingReference;
    @Column(nullable = false) private Long userId;

    @Column(nullable = false, unique = true, length = 20) private String tranId;

    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 3) private String currency;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PaymentMethod method;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PaymentStatus status;

    @Column(length = 50) private String providerRef;   // ABA apv
    @Column(length = 50) private String bankRef;
    @Column(length = 30) private String paymentType;   // ABA Pay, KHQR, VISA...

    private Instant expiresAt;
    private Instant paidAt;
    @Column(length = 255) private String failureReason;
    @Column(unique = true, length = 100) private String idempotencyKey;

    @Version private Long version;
    @CreationTimestamp
    private Instant createdAt;
    @UpdateTimestamp
    private Instant updatedAt;

}
