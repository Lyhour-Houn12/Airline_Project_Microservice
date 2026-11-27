package com.jing.booking.outbox;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OutboxRepository extends JpaRepository<OutboxMessage, Long> {

    @Query("""
        SELECT m.id FROM OutboxMessage m
        WHERE m.status = :pending AND m.nextAttemptAt <= :now
        ORDER BY m.id
        """)
    List<Long> findDueIds(@Param("pending") OutboxMessage.Status pending, @Param("now") Instant now, Pageable pageable);

    /**
     * Locks one PENDING message for sending. SKIP LOCKED (lock timeout -2): if another instance of this
     * service is already sending it, we get nothing instead of waiting, so a message is sent by one at a time.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("SELECT m FROM OutboxMessage m WHERE m.id = :id AND m.status = :pending")
    Optional<OutboxMessage> lockPending(@Param("id") Long id, @Param("pending") OutboxMessage.Status pending);

    long countByStatus(OutboxMessage.Status status);
}
