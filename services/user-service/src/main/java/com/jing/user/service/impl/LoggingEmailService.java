package com.jing.user.service.impl;

import com.jing.common.enumeration.OtpPurpose;
import com.jing.user.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * DEV ONLY: prints the code to the console so you can test without an SMTP server.
 * Active unless app.mail.enabled=true. Never run this in production (it logs the OTP).
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingEmailService implements EmailService {

    @Override
    public void sendOtp(String to, OtpPurpose purpose, String code, long expiryMinutes) {
        log.warn("[DEV EMAIL] to={} purpose={} code={} (expires in {} min)", to, purpose, code, expiryMinutes);
    }
}