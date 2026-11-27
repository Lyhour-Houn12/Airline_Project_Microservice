package com.jing.user.service;

import com.jing.common.dto.user.AuthResponse;
import com.jing.user.payload.request.LoginRequest;
import com.jing.user.payload.request.ResetPasswordRequest;
import com.jing.user.payload.request.SignupRequest;
import com.jing.user.payload.request.VerifyEmailRequest;

public interface AuthService {

    AuthResponse signup(SignupRequest request);

    AuthResponse verifyEmail(VerifyEmailRequest request);

    void resendVerification(String email);

    AuthResponse signin(LoginRequest request);

    AuthResponse refresh(String refreshToken);

    void logout(String refreshToken);

    void forgotPassword(String email);

    void resetPassword(ResetPasswordRequest request);
}
