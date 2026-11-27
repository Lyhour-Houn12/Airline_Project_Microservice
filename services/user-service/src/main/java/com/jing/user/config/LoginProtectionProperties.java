package com.jing.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.login-protection")
public record LoginProtectionProperties(
        int maxFailedAttempts,
        long lockoutMinutes) {
}