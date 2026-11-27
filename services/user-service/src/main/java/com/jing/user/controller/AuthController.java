package com.jing.user.controller;

import com.jing.common.dto.user.AuthResponse;
import com.jing.user.payload.request.*;
import com.jing.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<AuthResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return ResponseEntity.ok(authService.verifyEmail(request));
    }

    @PostMapping("/verify-email/resend")
    public ResponseEntity<Map<String, String>> resendVerification(@Valid @RequestBody EmailRequest request) {
        authService.resendVerification(request.email());
        return ResponseEntity.ok(Map.of("message", "If that account needs verification, a new code has been sent."));
    }

    @PostMapping("/signin")
    public ResponseEntity<AuthResponse> signin(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.signin(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody EmailRequest request) {
        authService.forgotPassword(request.email());
        return ResponseEntity.ok(Map.of("message", "If that email is registered, a reset code has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password updated. Please sign in."));
    }
}
