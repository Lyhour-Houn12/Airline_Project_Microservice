package com.jing.user.service;

import com.jing.common.enumeration.OtpPurpose;
import com.jing.user.model.User;

public interface OtpService {

    void send(User user, OtpPurpose purpose);

    void verify(User user, OtpPurpose purpose, String code);
}
