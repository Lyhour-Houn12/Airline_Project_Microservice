package com.jing.payment.payload.response;

import java.math.BigDecimal;
import java.util.Set;

/**
 * PayWay's answer to a refund request.
 *
 * @param code PayWay status.code: "00" is success, "PTL..." an error
 */
public record AbaRefundResult(
        String code,
        String message,
        BigDecimal totalRefunded,
        String transactionStatus,
        String raw
) {
    /** Errors worth retrying later: busy, or not enough balance on the merchant account yet. */
    private static final Set<String> TEMPORARY = Set.of("PTL168", "PTL181");

    public boolean succeeded() {
        return "00".equals(code);
    }

    public boolean temporary() {
        return TEMPORARY.contains(code);
    }
}
