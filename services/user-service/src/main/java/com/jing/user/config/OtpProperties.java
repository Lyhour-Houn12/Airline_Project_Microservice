package com.jing.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.otp")
public record OtpProperties(
        long expiryMinutes,
        int maxAttempts,
        long resendCooldownSeconds) {
}