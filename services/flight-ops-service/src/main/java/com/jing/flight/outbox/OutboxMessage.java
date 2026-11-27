package com.jing.flight.outbox;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * A message to another service, saved in the SAME transaction as the change that caused it
 * (transactional outbox). If the change is committed, the message is too, and the dispatcher keeps
 * delivering it until the other service accepts it. Receivers must handle repeats (they do).
 */
@Entity
@Table(name = "outbox_messages", indexes = @Index(name = "idx_outbox_due", columnList = "status, nextAttemptAt"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxMessage {

    public enum Status { PENDING, SENT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Which handler delivers it, e.g. "PAYMENT_RESULT". */
    @Column(nullable = false, length = 40)
    private String type;

    /** The record it is about (payment id, booking id...), for searching and logs. */
    @Column(nullable = false)
    private Long aggregateId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private Status status = Status.PENDING;

    @Builder.Default
    private int attempts = 0;

    @Column(nullable = false)
    private Instant nextAttemptAt;

    @Column(length = 500)
    private String lastError;

    @CreationTimestamp
    private Instant createdAt;

    private Instant sentAt;
}
