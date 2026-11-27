package com.jing.user.config;

import com.jing.user.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class JwtProvider {
    private final JwtProperties props;
    private final SecretKey key;

    public JwtProvider(JwtProperties props) {
        this.props = props;
        if(props.secret() == null || props.secret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 byte (set JWT_SECRET)");
        }
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user){
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(props.issuer())
                .subject(String.valueOf(user.getId()))
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(props.accessTokenMinutes(), ChronoUnit.MINUTES)))
                .claim("email", user.getEmail())
                .claim("roles", List.of(user.getRole()))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(props.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


}
