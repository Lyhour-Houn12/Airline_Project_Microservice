package com.jing.gateway.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * gateway.* settings.
 *
 * @param jwtSecret     the same JWT_SECRET user-service signs tokens with
 * @param jwtIssuer     the issuer user-service puts in its tokens
 * @param blockedPaths  paths that exist on a service but must not be reachable from outside
 *                      (service-to-service lookups). /internal/** is always blocked.
 * @param anonymousOnly paths where an Authorization header is ignored instead of verified
 *                      (e.g. PayWay's callback, which never carries our token)
 */
@ConfigurationProperties(prefix = "gateway")
public record GatewayProperties(
        String jwtSecret,
        @DefaultValue("user-service") String jwtIssuer,
        @DefaultValue List<String> blockedPaths,
        @DefaultValue List<String> anonymousOnly
) {
}
