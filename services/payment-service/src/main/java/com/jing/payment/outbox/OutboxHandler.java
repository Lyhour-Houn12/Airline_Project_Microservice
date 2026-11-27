package com.jing.payment.outbox;

/**
 * Delivers one kind of outbox message. Throw to have it retried later; return normally when the
 * other service has accepted it. Must be safe to run more than once for the same message.
 */
public interface OutboxHandler {

    /** The message type this handler delivers, e.g. "PAYMENT_RESULT". */
    String type();

    void deliver(OutboxMessage message) throws Exception;
}
