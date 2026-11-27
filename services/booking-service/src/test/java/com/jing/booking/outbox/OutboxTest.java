package com.jing.booking.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OutboxTest {

    private final OutboxRepository repository = mock(OutboxRepository.class);
    private final PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);
    private final AtomicInteger calls = new AtomicInteger();
    private boolean otherServiceUp = false;

    private final OutboxHandler handler = new OutboxHandler() {
        @Override public String type() { return "TEST"; }
        @Override public void deliver(OutboxMessage m) {
            calls.incrementAndGet();
            if (!otherServiceUp) throw new IllegalStateException("Connection refused");
        }
    };
    private final Outbox outbox = new Outbox(repository, List.of(handler), JsonMapper.builder().build(), txManager);
    private final OutboxMessage message = OutboxMessage.builder().id(1L).type("TEST").aggregateId(7L)
            .payload("{}").nextAttemptAt(Instant.now()).build();

    @BeforeEach
    void setUp() {
        when(txManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(repository.findById(1L)).thenReturn(Optional.of(message));
        when(repository.lockPending(eq(1L), eq(OutboxMessage.Status.PENDING)))
                .thenAnswer(inv -> message.getStatus() == OutboxMessage.Status.PENDING ? Optional.of(message) : Optional.empty());
    }

    @Test
    void aFailedDeliveryStaysPendingAndIsRetriedLater() {
        Instant before = Instant.now();

        outbox.dispatch(1L);

        assertThat(message.getStatus()).isEqualTo(OutboxMessage.Status.PENDING);
        assertThat(message.getAttempts()).isEqualTo(1);
        assertThat(message.getLastError()).contains("Connection refused");
        assertThat(message.getNextAttemptAt()).isAfter(before.plusSeconds(4));
    }

    @Test
    void itIsDeliveredOnceTheOtherServiceIsBack() {
        outbox.dispatch(1L);                    // down
        otherServiceUp = true;
        outbox.dispatch(1L);                    // back

        assertThat(message.getStatus()).isEqualTo(OutboxMessage.Status.SENT);
        assertThat(message.getSentAt()).isNotNull();
        assertThat(message.getLastError()).isNull();

        outbox.dispatch(1L);                    // already sent: not sent again
        assertThat(calls).hasValue(2);
    }

    @Test
    void backoffGrowsAndIsCapped() {
        assertThat(Outbox.backoff(1)).isEqualTo(Duration.ofSeconds(5));
        assertThat(Outbox.backoff(2)).isEqualTo(Duration.ofSeconds(10));
        assertThat(Outbox.backoff(4)).isEqualTo(Duration.ofSeconds(40));
        assertThat(Outbox.backoff(12)).isEqualTo(Duration.ofMinutes(10));
        assertThat(Outbox.backoff(500)).isEqualTo(Duration.ofMinutes(10));
    }

    @Test
    void enqueueOutsideATransactionIsAProgrammingError() {
        assertThatThrownBy(() -> outbox.enqueue("TEST", 7L, "x")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> outbox.enqueue("UNKNOWN", 7L, "x")).isInstanceOf(IllegalArgumentException.class);
    }
}
