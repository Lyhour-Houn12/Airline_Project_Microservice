package com.jing.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment.aba")
public record AbaProperties(String baseUrl, String merchantId, String apiKey, String returnUrl, String continueSuccessUrl) {
}
