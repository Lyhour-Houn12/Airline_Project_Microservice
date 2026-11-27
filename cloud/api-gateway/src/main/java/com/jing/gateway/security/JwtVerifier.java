package com.jing.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

/**
 * Verifies the access tokens user-service issues (HMAC-signed with JWT_SECRET, issuer "user-service",
 * subject = user id, claim "roles" = [the user's role]). Expired, forged or foreign tokens are rejected.
 */
@Component
public class JwtVerifier {

    /** Same values as com.jing.common.enumeration.UserRole (the gateway doesn't depend on common-lib). */
    static final Set<String> KNOWN_ROLES = Set.of("ROLE_SYSTEM_ADMIN", "ROLE_AIRLINE_OWNER", "ROLE_CUSTOMER");

    private final SecretKey key;
    private final String issuer;

    public JwtVerifier(GatewayProperties props) {
        if (props.jwtSecret() == null || props.jwtSecret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("gateway.jwt-secret must be set to user-service's JWT_SECRET (at least 32 bytes)");
        }
        this.key = Keys.hmacShaKeyFor(props.jwtSecret().getBytes(StandardCharsets.UTF_8));
        this.issuer = props.jwtIssuer();
    }

    public record Identity(String userId, String role) {}

    /** @throws InvalidTokenException with a client-safe message when the token can't be trusted */
    public Identity verify(String token) {
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw new InvalidTokenException("Access token expired");
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("Invalid access token");
        }

        String subject = claims.getSubject();
        if (subject == null || !subject.matches("\\d+")) {
            throw new InvalidTokenException("Invalid access token");
        }
        List<?> roles = claims.get("roles", List.class);
        String role = roles == null || roles.isEmpty() ? null : String.valueOf(roles.getFirst());
        if (role == null || !KNOWN_ROLES.contains(role)) {
            throw new InvalidTokenException("Invalid access token");
        }
        return new Identity(subject, role);
    }

    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) {
            super(message);
        }
    }
}
