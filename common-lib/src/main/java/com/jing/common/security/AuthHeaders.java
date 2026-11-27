package com.jing.common.security;

/**
 * Identity headers that the API gateway adds after it has verified the caller's JWT.
 *
 * <p>The business services trust these headers, so the gateway MUST strip any
 * X-User-* header sent by the client before adding its own, and the services
 * must not be reachable from outside except through the gateway.
 */
public final class AuthHeaders {

    public static final String USER_ID = "X-User-Id";
    public static final String USER_ROLE = "X-User-Role";

    private AuthHeaders() {}
}
