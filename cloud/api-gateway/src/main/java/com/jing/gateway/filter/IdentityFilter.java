package com.jing.gateway.filter;

import com.jing.gateway.security.GatewayProperties;
import com.jing.gateway.security.JwtVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

/**
 * The gateway's one security job, in this order, for every request:
 * <ol>
 *   <li><b>Block</b> service-to-service paths ({@code /internal/**} and gateway.blocked-paths): 404.</li>
 *   <li><b>Strip</b> any X-User-Id / X-User-Role the client sent. Only this filter may set them.</li>
 *   <li><b>Authenticate</b>: a Bearer token is verified; valid -> X-User-Id / X-User-Role are set from it,
 *       invalid or expired -> 401. No token -> the request goes on anonymously.</li>
 * </ol>
 * <b>Authorization stays in the services</b> ({@code @RequireRole}, {@code CurrentUser}, ownership checks):
 * they know which endpoints are public and who owns what. The gateway only guarantees that the identity
 * they receive is real.
 */
@Component
public class IdentityFilter implements GlobalFilter, Ordered {

    public static final String USER_ID = "X-User-Id";
    public static final String USER_ROLE = "X-User-Role";
    private static final String BEARER = "Bearer ";
    private static final Logger log = LoggerFactory.getLogger(IdentityFilter.class);

    private final JwtVerifier jwtVerifier;
    private final List<String> blocked;
    private final List<String> anonymousOnly;
    private final AntPathMatcher matcher = new AntPathMatcher();

    public IdentityFilter(JwtVerifier jwtVerifier, GatewayProperties props) {
        this.jwtVerifier = jwtVerifier;
        this.blocked = props.blockedPaths();
        this.anonymousOnly = props.anonymousOnly();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;   // before routing and every other filter
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        if (path.startsWith("/internal/") || matchesAny(blocked, path)) {
            return error(exchange, HttpStatus.NOT_FOUND, "No endpoint " + request.getMethod() + " " + path);
        }

        ServerHttpRequest.Builder forwarded = request.mutate().headers(h -> {
            h.remove(USER_ID);
            h.remove(USER_ROLE);
        });

        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith(BEARER) && !matchesAny(anonymousOnly, path)) {
            try {
                JwtVerifier.Identity identity = jwtVerifier.verify(authorization.substring(BEARER.length()).trim());
                forwarded.headers(h -> {
                    h.set(USER_ID, identity.userId());
                    h.set(USER_ROLE, identity.role());
                });
            } catch (JwtVerifier.InvalidTokenException e) {
                log.debug("Rejected {} {}: {}", request.getMethod(), path, e.getMessage());
                return error(exchange, HttpStatus.UNAUTHORIZED, e.getMessage());
            }
        }
        return chain.filter(exchange.mutate().request(forwarded.build()).build());
    }

    private boolean matchesAny(List<String> patterns, String path) {
        return patterns.stream().anyMatch(p -> matcher.match(p, path));
    }

    /** Same JSON shape as the services' ErrorResponse, so clients handle one error format. */
    static Mono<Void> error(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        if (status == HttpStatus.UNAUTHORIZED) {
            response.getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }
        String body = "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}"
                .formatted(Instant.now(), status.value(), status.getReasonPhrase(),
                        json(message), json(exchange.getRequest().getPath().value()));
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8))));
    }

    private static String json(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
