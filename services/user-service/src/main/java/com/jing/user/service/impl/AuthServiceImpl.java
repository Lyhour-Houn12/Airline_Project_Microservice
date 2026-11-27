package com.jing.user.service.impl;


import com.jing.common.enumeration.OtpPurpose;
import com.jing.common.enumeration.UserRole;
import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.ForbiddenException;
import com.jing.common.exception.TooManyRequestException;
import com.jing.common.exception.UnauthorizedException;
import com.jing.common.dto.user.AuthResponse;
import com.jing.user.config.JwtProvider;
import com.jing.user.config.LoginProtectionProperties;
import com.jing.user.mapper.UserMapper;
import com.jing.user.model.RefreshToken;
import com.jing.user.model.User;
import com.jing.user.payload.request.LoginRequest;
import com.jing.user.payload.request.ResetPasswordRequest;
import com.jing.user.payload.request.SignupRequest;
import com.jing.user.payload.request.VerifyEmailRequest;
import com.jing.user.repository.UserRepository;
import com.jing.user.service.AuthService;
import com.jing.user.service.OtpService;
import com.jing.user.service.RefreshTokenService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    public static final String INVALID_CREDENTIALS = "Invalid email or password";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final OtpService otpService;
    private final LoginProtectionProperties protection;


    private String dummyHash;

    @PostConstruct
    public void init(){
        this.dummyHash =passwordEncoder.encode(UUID.randomUUID().toString());
    }


    @Override
    @Transactional(noRollbackFor =  BadRequestException.class)
    public AuthResponse signup(SignupRequest request) {
        String email = normalizeEmail(request.email());
        Optional<User> existing = userRepository.findByEmail(email);
        User user;
        if (existing.isPresent()) {
            user = existing.get();
            if(user.isVerified())
                throw new BadRequestException("Email already registered.");
            user.setFullname(request.fullname().trim());
            user.setPassword(passwordEncoder.encode(request.password()));
            user.setPhone(request.phone());
        }else{
            try{
                user = userRepository.saveAndFlush(User.builder()
                                .fullname(request.fullname().trim())
                                .email(email)
                                .password(passwordEncoder.encode(request.password()))
                                .phone(request.phone())
                                .role(UserRole.ROLE_CUSTOMER)
                        .build());
            }catch (DataIntegrityViolationException e){
                throw new BadRequestException("Email already registered.");
            }
        }
        otpService.send(user, OtpPurpose.EMAIL_VERIFICATION);

        AuthResponse authResponse = new AuthResponse();
        authResponse.setTitle("Verify your email");
        authResponse.setMessage("We've sent a 6-digit code to " + email + ".");
        authResponse.setUser(UserMapper.toDto(user));
        return authResponse;
    }

    @Override
    @Transactional(noRollbackFor =  BadRequestException.class)
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new BadRequestException(OtpServiceImpl.INVALID_CODE));

        if(user.isVerified())
            throw new BadRequestException("Email already verified.");

        otpService.verify(user, OtpPurpose.EMAIL_VERIFICATION,  request.code());

        user.setVerified(true);
        user.setLastLogin(LocalDateTime.now());
        return buildAuthResponse(user, "Email Verified", "Welcome " + user.getFullname());
    }

    @Override
    @Transactional(noRollbackFor =  BadRequestException.class)
    public void resendVerification(String email) {
        userRepository.findByEmail(normalizeEmail(email))
                .filter(u -> !u.isVerified())
                .ifPresent(u -> sendQuietly(u, OtpPurpose.EMAIL_VERIFICATION));
    }

    @Override
    @Transactional(noRollbackFor = {UnauthorizedException.class, ForbiddenException.class, TooManyRequestException.class})
    public AuthResponse signin(LoginRequest request) {
        LocalDateTime now = LocalDateTime.now();
        User user = userRepository.findByEmail(normalizeEmail(request.email())).orElse(null);

        if(user != null && user.getLockedUntil() != null){
            if(user.getLockedUntil().isAfter(now)) {
                long minutes = Duration.between(now, user.getLockedUntil()).toMinutes() + 1;
                throw new TooManyRequestException("Too many attempts. Try again in " + minutes + " minute(s).");
            }
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);
        }

        // always run one BCrypt comparison, and give one generic error for both failure cases
        boolean passwordOk = passwordEncoder.matches(request.password(), user != null ? user.getPassword() : dummyHash);

        if (user == null){
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }

        if (!passwordOk){
            int attempts = user.getFailedLoginAttempts() + 1;
            if (attempts >= protection.maxFailedAttempts()){
                user.setLockedUntil(now.plusMinutes(protection.lockoutMinutes()));
                user.setFailedLoginAttempts(0);
            }else{
                user.setFailedLoginAttempts(attempts);
            }
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }
        user.setLockedUntil(null);
        user.setFailedLoginAttempts(0);

        if(!user.isVerified()) {
            throw new ForbiddenException("Email not verified. Please verify email first.");
        }
        user.setLastLogin(now);
        return buildAuthResponse(user, "Login Successful", "Welcome back " + user.getFullname());
    }

    @Override
    @Transactional(noRollbackFor =  UnauthorizedException.class)
    public AuthResponse refresh(String refreshToken) {
        RefreshToken stored  = refreshTokenService.consume(refreshToken);
        return buildAuthResponse(stored.getUser(), "Token refreshed", "Your session has been renewed.");
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmail(normalizeEmail(email))
                .ifPresent(u -> sendQuietly(u, OtpPurpose.PASSWORD_RESET));
    }

    @Override
    @Transactional(noRollbackFor =  BadRequestException.class)
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new BadRequestException(OtpServiceImpl.INVALID_CODE));

        otpService.verify(user, OtpPurpose.PASSWORD_RESET, request.code());

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setVerified(true);                 // they proved they own the mailbox
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        refreshTokenService.revokeAll(user.getId());   // log out every device
    }

    private void sendQuietly(User user, OtpPurpose purpose) {
        try {
            otpService.send(user, purpose);
        }catch (TooManyRequestException ignore){

        }
    }

    private AuthResponse buildAuthResponse(User user, String title, String message){
        return AuthResponse.builder()
                .title(title)
                .message(message)
                .jwt(jwtProvider.generateAccessToken(user))
                .refreshToken(refreshTokenService.issue(user))
                .user(UserMapper.toDto(user))
                .build();
    }


    private static String normalizeEmail(String email){
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
