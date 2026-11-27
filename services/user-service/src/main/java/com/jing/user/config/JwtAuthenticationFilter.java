package com.jing.user.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;


public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    public JwtAuthenticationFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        // no token (public endpoints like /auth/signup): skip, let the request continue unauthenticated
        if (header == null || !header.startsWith(PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = header.substring(PREFIX.length());
            Claims claims = jwtProvider.parse(token);
            Long userId = Long.valueOf(claims.getSubject());

            List<?> roles = claims.get("roles", List.class);
            List<SimpleGrantedAuthority> authorities = roles == null ? List.of()
                    : roles.stream().map(r -> new SimpleGrantedAuthority(String.valueOf(r))).toList();

            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(userId, null, authorities));
        } catch (JwtException | IllegalArgumentException e) {
            // invalid or expired token: stay unauthenticated, the entry point answers 401
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);


    }
}
