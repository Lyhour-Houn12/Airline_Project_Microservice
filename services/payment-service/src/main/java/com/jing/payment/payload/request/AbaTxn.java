package com.jing.payment.payload.request;

import java.math.BigDecimal;

public record AbaTxn(boolean found, Integer statusCode, BigDecimal totalAmount, String apv, String raw) {
}
