package com.jing.user.service;

import com.jing.common.enumeration.OtpPurpose;

public interface EmailService {

    void sendOtp(String to, OtpPurpose otpPurpose, String code, long expiryMinutes);
}
