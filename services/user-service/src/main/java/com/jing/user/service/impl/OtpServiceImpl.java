package com.jing.user.service.impl;

import com.jing.common.enumeration.OtpPurpose;
import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.TooManyRequestException;
import com.jing.user.config.OtpProperties;
import com.jing.user.model.OtpCode;
import com.jing.user.model.User;
import com.jing.user.repository.OtpCodeRepository;
import com.jing.user.service.EmailService;
import com.jing.user.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {
    public static final String INVALID_CODE = "Invalid or expired code.";

    private final OtpCodeRepository otpCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpProperties otpProperties;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();

    @Override
    public void send(User user, OtpPurpose purpose) {
        Instant now = Instant.now();
        otpCodeRepository.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(user.getId(), purpose).ifPresent(last -> {
            long wait = otpProperties.resendCooldownSeconds() - Duration.between(last.getCreatedAt(), now).getSeconds();
            if(wait > 0){
                throw new TooManyRequestException("Please wait " + wait + " seconds before requesting a new code.");
            }
        });
        otpCodeRepository.consumeAll(user.getId(), purpose);
        String code = String.format("%06d", random.nextInt(1_000_000));
        otpCodeRepository.save(OtpCode.builder()
                        .user(user)
                        .purpose(purpose)
                        .codeHash(passwordEncoder.encode(code))
                        .createdAt(now)
                        .expiresAt(now.plus(otpProperties.expiryMinutes(), ChronoUnit.MINUTES))
                .build());
        emailService.sendOtp(user.getEmail(), purpose, code,  otpProperties.expiryMinutes());
    }

    @Override
    public void verify(User user, OtpPurpose purpose, String code) {
        OtpCode otp =  otpCodeRepository.findFirstByUserIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(user.getId(), purpose)
                .orElseThrow(() -> new BadRequestException(INVALID_CODE));
        if(otp.getExpiresAt().isBefore(Instant.now())){
            otp.setConsumed(true);
            throw new BadRequestException(INVALID_CODE);
        }
        if (otp.getAttempts() >= otpProperties.maxAttempts()){
            otp.setConsumed(true);
            throw new BadRequestException("Too many wrong attempts. Please request a new code.");
        }
        otp.setAttempts(otp.getAttempts() + 1);
        if(!passwordEncoder.matches(code, otp.getCodeHash())){
            throw new BadRequestException(INVALID_CODE);
        }
        otp.setConsumed(true);
    }


}
