package com.jing.payment.payload.request;

import java.math.BigDecimal;

/** check-transaction result. refundAmount: how much PayWay has refunded so far (0 if none). */
public record AbaTxn(boolean found, Integer statusCode, BigDecimal totalAmount, String apv,
                     BigDecimal refundAmount, String raw) {
}
