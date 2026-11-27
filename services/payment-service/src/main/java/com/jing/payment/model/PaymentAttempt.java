package com.jing.payment.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "payment_attempts")
@Getter
@Setter
@NoArgsConstructor
public class PaymentAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private Long paymentId;
    @Column(nullable = false, length = 30) private String type;   // CHECKOUT_CREATED, CALLBACK, STATUS_CHECK
    @Column(columnDefinition = "TEXT") private String payload;
    @CreationTimestamp
    private Instant createdAt;

    public static PaymentAttempt of(Long paymentId, String type, String payload) {
        PaymentAttempt a = new PaymentAttempt();
        a.paymentId = paymentId; a.type = type; a.payload = payload;
        return a;
    }
}