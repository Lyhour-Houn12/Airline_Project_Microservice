package com.jing.common.security;

import com.jing.common.enumeration.UserRole;
import com.jing.common.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Reads and validates the identity headers. The result is cached on the request, so the
 * role interceptor and the argument resolver parse the headers only once.
 */
public final class CurrentUserResolver {

    private static final String ATTRIBUTE = CurrentUserResolver.class.getName() + ".user";

    private CurrentUserResolver() {}

    /**
     * @param required when true, missing headers give a 401; when false they give {@link CurrentUser#ANONYMOUS}
     * @throws UnauthorizedException if the headers are present but malformed, or missing while required
     */
    public static CurrentUser resolve(HttpServletRequest request, boolean required) {
        CurrentUser user = (CurrentUser) request.getAttribute(ATTRIBUTE);
        if (user == null) {
            user = parse(request.getHeader(AuthHeaders.USER_ID), request.getHeader(AuthHeaders.USER_ROLE));
            request.setAttribute(ATTRIBUTE, user);
        }
        if (required && !user.isAuthenticated()) {
            throw new UnauthorizedException("Authentication required");
        }
        return user;
    }

    static CurrentUser parse(String rawId, String rawRole) {
        boolean noId = rawId == null || rawId.isBlank();
        boolean noRole = rawRole == null || rawRole.isBlank();
        if (noId && noRole) {
            return CurrentUser.ANONYMOUS;
        }
        if (noId || noRole) {
            throw new UnauthorizedException("Both " + AuthHeaders.USER_ID + " and "
                    + AuthHeaders.USER_ROLE + " headers are required");
        }

        long id;
        try {
            id = Long.parseLong(rawId.trim());
        } catch (NumberFormatException e) {
            throw new UnauthorizedException("Invalid " + AuthHeaders.USER_ID + " header");
        }
        if (id <= 0) {
            throw new UnauthorizedException("Invalid " + AuthHeaders.USER_ID + " header");
        }

        UserRole role;
        try {
            role = UserRole.valueOf(rawRole.trim());
        } catch (IllegalArgumentException e) {
            throw new UnauthorizedException("Invalid " + AuthHeaders.USER_ROLE + " header");
        }
        return new CurrentUser(id, role);
    }
}
