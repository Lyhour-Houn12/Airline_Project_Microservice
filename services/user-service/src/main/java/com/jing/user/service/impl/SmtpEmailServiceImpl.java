package com.jing.user.service.impl;

import com.jing.common.enumeration.OtpPurpose;
import com.jing.user.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class SmtpEmailServiceImpl implements EmailService {
    private final JavaMailSender mailSender;
    private final String from;

    public SmtpEmailServiceImpl(JavaMailSender mailSender, @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void sendOtp(String to, OtpPurpose purpose, String code, long expiryMinutes) {
        String subject = purpose == OtpPurpose.EMAIL_VERIFICATION
                ? "Verify your email"
                : "Reset your password";
        String body = "Your verification code is " + code + ".\n"
                + "It expires in " + expiryMinutes + " minutes. "
                + "If you didn't request this, you can ignore this email.";
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
