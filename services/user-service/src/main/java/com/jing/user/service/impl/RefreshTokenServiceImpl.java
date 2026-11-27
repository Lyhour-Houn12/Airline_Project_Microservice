package com.jing.user.service.impl;

import com.jing.common.exception.UnauthorizedException;
import com.jing.user.config.JwtProperties;
import com.jing.user.model.RefreshToken;
import com.jing.user.model.User;
import com.jing.user.repository.RefreshTokenRepository;
import com.jing.user.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final SecureRandom random = new SecureRandom();

    @Override
    public String issue(User user) {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        String raw = Base64.getEncoder().encodeToString(bytes);

        refreshTokenRepository.save(RefreshToken.builder()
                        .user(user)
                        .tokenHash(sh256(raw))
                        .expiresAt(Instant.now().plus(jwtProperties.refreshTokenDays(), ChronoUnit.DAYS))
                .build());
        return raw;
    }
    /**
     * Validates and revokes the token (rotation: every refresh token works exactly once).
     * Presenting an already-used token means it probably leaked, so all sessions are revoked.
     */
    @Override
    public RefreshToken consume(String raw) {
        RefreshToken token = refreshTokenRepository.findByTokenHash(sh256(raw))
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token."));
        if(token.isRevoked()){
            refreshTokenRepository.revokeAllByUserId(token.getUser().getId());
            throw new UnauthorizedException("Refresh token was already used. Please sign in again.");
        }
        if(token.getExpiresAt().isBefore(Instant.now())){
            throw new UnauthorizedException("Refresh token expired. Please sign in again.");
        }
        token.setRevoked(true);
        return token;
    }

    @Override
    public void revoke(String raw) {
        refreshTokenRepository.findByTokenHash(sh256(raw)).ifPresent(t -> t.setRevoked(true));
    }

    @Override
    public void revokeAll(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    private static String sh256(String value){
        try{
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        }catch (NoSuchAlgorithmException e){
            throw new IllegalStateException(e);
        }
    }
}
