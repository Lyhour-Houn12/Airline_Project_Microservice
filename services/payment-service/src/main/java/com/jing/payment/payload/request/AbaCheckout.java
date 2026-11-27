package com.jing.payment.payload.request;

import java.util.Map;

public record AbaCheckout(String actionUrl, Map<String, String> fields) {}