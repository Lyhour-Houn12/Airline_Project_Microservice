package com.jing.gateway.filter;

import com.jing.gateway.security.GatewayProperties;
import com.jing.gateway.security.JwtVerifier;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityFilterTest {

    private static final String SECRET = "a-test-secret-that-is-definitely-longer-than-sixty-four-bytes-for-hs512!!";

    private final GatewayProperties props = new GatewayProperties(SECRET, "user-service",
            List.of("/api/v1/flights/by-ids"), List.of("/api/v1/payments/aba/callback"));
    private final IdentityFilter filter = new IdentityFilter(new JwtVerifier(props), props);

    /** Runs the filter; returns the request the service would receive (null when the gateway answered itself). */
    private ServerHttpRequest forwarded(MockServerWebExchange exchange) {
        AtomicReference<ServerHttpRequest> passed = new AtomicReference<>();
        GatewayFilterChain chain = ex -> {
            passed.set(ex.getRequest());
            return Mono.empty();
        };
        filter.filter(exchange, chain).block();
        return passed.get();
    }

    private static String token(String subject, String role, String issuer, Instant expires, String secret) {
        return Jwts.builder()
                .issuer(issuer)
                .subject(subject)
                .claim("roles", List.of(role))
                .expiration(Date.from(expires))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private static String validToken(String subject, String role) {
        return token(subject, role, "user-service", Instant.now().plus(15, ChronoUnit.MINUTES), SECRET);
    }

    @Test
    void forgedIdentityHeadersAreRemoved() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/bookings")
                .header("X-User-Id", "14").header("X-User-Role", "ROLE_SYSTEM_ADMIN"));

        ServerHttpRequest out = forwarded(exchange);

        assertThat(out.getHeaders().containsHeader("X-User-Id")).isFalse();
        assertThat(out.getHeaders().containsHeader("X-User-Role")).isFalse();
    }

    @Test
    void validTokenBecomesIdentityHeaders() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/bookings")
                .header("Authorization", "Bearer " + validToken("15", "ROLE_CUSTOMER"))
                .header("X-User-Role", "ROLE_SYSTEM_ADMIN"));          // a forged role on top is ignored

        ServerHttpRequest out = forwarded(exchange);

        assertThat(out.getHeaders().getFirst("X-User-Id")).isEqualTo("15");
        assertThat(out.getHeaders().get("X-User-Role")).containsExactly("ROLE_CUSTOMER");
        assertThat(out.getHeaders().getFirst("Authorization")).startsWith("Bearer ");   // user-service needs it
    }

    @Test
    void badTokensGet401() {
        Instant soon = Instant.now().plus(15, ChronoUnit.MINUTES);
        List<String> bad = List.of(
                "not-a-jwt",
                token("15", "ROLE_CUSTOMER", "user-service", Instant.now().minus(1, ChronoUnit.MINUTES), SECRET), // expired
                token("15", "ROLE_CUSTOMER", "someone-else", soon, SECRET),                                     // wrong issuer
                token("15", "ROLE_CUSTOMER", "user-service", soon, SECRET.replace('a', 'b')),                   // wrong key
                token("15", "ROLE_GOD", "user-service", soon, SECRET));                                         // unknown role

        for (String t : bad) {
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/api/v1/bookings").header("Authorization", "Bearer " + t));

            assertThat(forwarded(exchange)).as(t).isNull();
            assertThat(exchange.getResponse().getStatusCode()).as(t).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Test
    void internalAndBlockedPathsAreNotReachable() {
        for (String path : List.of("/internal/v1/bookings/7/payment-result", "/api/v1/flights/by-ids")) {
            MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post(path)
                    .header("Authorization", "Bearer " + validToken("14", "ROLE_SYSTEM_ADMIN")));

            assertThat(forwarded(exchange)).as(path).isNull();
            assertThat(exchange.getResponse().getStatusCode()).as(path).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Test
    void anonymousRequestsPassThrough() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/flight-instances"));

        assertThat(forwarded(exchange)).isNotNull();
    }

    @Test
    void payWayCallbackIgnoresAnyAuthorizationHeader() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/payments/aba/callback")
                .header("Authorization", "Bearer garbage"));

        ServerHttpRequest out = forwarded(exchange);

        assertThat(out).isNotNull();
        assertThat(out.getHeaders().containsHeader("X-User-Id")).isFalse();
    }
}
