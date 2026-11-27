package com.jing.user.service;

import com.jing.user.model.RefreshToken;
import com.jing.user.model.User;

public interface RefreshTokenService {
    String issue(User user);

    RefreshToken consume(String raw);

    void revoke(String raw);

    void revokeAll(Long userId);
}
