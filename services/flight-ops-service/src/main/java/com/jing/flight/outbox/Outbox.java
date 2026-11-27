package com.jing.flight.outbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Transactional outbox: reliable messages to other services.
 *
 * <ol>
 *   <li>{@link #enqueue} saves the message in the caller's transaction: no change without its message,
 *       no message without its change.</li>
 *   <li>Right after that transaction commits, the message is sent once (so normally nothing is slower).</li>
 *   <li>If sending fails, {@link #dispatchDue} retries it every few seconds, backing off up to
 *       {@value #MAX_BACKOFF_MINUTES} minutes, and never gives up. After {@value #ALERT_AFTER} attempts
 *       every failure is logged as an error so someone looks at it.</li>
 * </ol>
 */
@Slf4j
@Component
public class Outbox {

    static final int MAX_BACKOFF_MINUTES = 10;
    static final int ALERT_AFTER = 10;
    private static final Duration FIRST_BACKOFF = Duration.ofSeconds(5);
    private static final int BATCH = 50;

    private final OutboxRepository repository;
    private final Map<String, OutboxHandler> handlers;
    private final ObjectMapper mapper;
    private final TransactionTemplate newTx;

    public Outbox(OutboxRepository repository, List<OutboxHandler> handlers, ObjectMapper mapper,
                  PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.handlers = handlers.stream().collect(Collectors.toMap(OutboxHandler::type, Function.identity()));
        this.mapper = mapper;
        this.newTx = new TransactionTemplate(transactionManager);
        this.newTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Must be called inside the transaction that makes the change the message is about. */
    public void enqueue(String type, Long aggregateId, Object payload) {
        if (!handlers.containsKey(type)) {
            throw new IllegalArgumentException("No outbox handler for type " + type);
        }
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Outbox.enqueue must run inside a transaction");
        }
        OutboxMessage message = repository.save(OutboxMessage.builder()
                .type(type)
                .aggregateId(aggregateId)
                .payload(mapper.writeValueAsString(payload))
                .nextAttemptAt(Instant.now())
                .build());
        Long id = message.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                dispatch(id);            // first try right away; the job picks it up if this fails
            }
        });
    }


    @Scheduled(fixedDelayString = "${outbox.poll-millis:5000}", initialDelayString = "${outbox.poll-millis:5000}")
    public void dispatchDue() {
        List<Long> due = repository.findDueIds(OutboxMessage.Status.PENDING, Instant.now(), PageRequest.of(0, BATCH));
        due.forEach(this::dispatch);
    }

    /** Sends one message if it is still PENDING and nobody else is sending it. Never throws. */
    void dispatch(Long id) {
        try {
            newTx.executeWithoutResult(status -> {
                OutboxMessage message = repository.lockPending(id, OutboxMessage.Status.PENDING).orElse(null);
                if (message == null) return;           // already sent, or another instance has it
                try {
                    handlers.get(message.getType()).deliver(message);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                message.setStatus(OutboxMessage.Status.SENT);
                message.setSentAt(Instant.now());
                message.setAttempts(message.getAttempts() + 1);
                message.setLastError(null);
            });
        } catch (RuntimeException e) {
            recordFailure(id, e);
        }
    }

    private void recordFailure(Long id, RuntimeException e) {
        try {
            newTx.executeWithoutResult(status -> repository.findById(id).ifPresent(m -> {
                int attempts = m.getAttempts() + 1;
                m.setAttempts(attempts);
                m.setNextAttemptAt(Instant.now().plus(backoff(attempts)));
                String error = String.valueOf(e.getMessage());
                m.setLastError(error.substring(0, Math.min(500, error.length())));
                if (attempts >= ALERT_AFTER) {
                    log.error("Outbox message {} ({} for {}) still not delivered after {} attempts: {}",
                            id, m.getType(), m.getAggregateId(), attempts, error);
                } else {
                    log.warn("Outbox message {} ({} for {}) failed, attempt {}: {}",
                            id, m.getType(), m.getAggregateId(), attempts, error);
                }
            }));
        } catch (RuntimeException inner) {
            log.error("Could not record the failure of outbox message {}", id, inner);
        }
    }

    /** 5 s, 10 s, 20 s, 40 s ... capped at 10 minutes. */
    static Duration backoff(int attempts) {
        Duration max = Duration.ofMinutes(MAX_BACKOFF_MINUTES);
        if (attempts >= 20) return max;
        Duration d = FIRST_BACKOFF.multipliedBy(1L << Math.max(0, attempts - 1));
        return d.compareTo(max) > 0 ? max : d;
    }
}
