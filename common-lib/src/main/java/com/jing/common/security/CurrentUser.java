package com.jing.common.security;

import com.jing.common.enumeration.UserRole;

import java.util.Arrays;

/**
 * The caller of the current request, built from the gateway headers.
 *
 * <p>Declare it as a controller parameter and it is filled in automatically:
 * <pre>{@code
 * @PostMapping
 * public FooResponse create(CurrentUser user, @Valid @RequestBody FooRequest request) { ... }
 *
 * // public endpoint: anonymous callers get CurrentUser.ANONYMOUS instead of a 401
 * @GetMapping
 * public Page<Foo> list(@AuthUser(required = false) CurrentUser user) { ... }
 * }</pre>
 */
public record CurrentUser(Long id, UserRole role) {

    /** Used for public endpoints when no identity headers were sent. */
    public static final CurrentUser ANONYMOUS = new CurrentUser(null, null);

    public boolean isAuthenticated() {
        return id != null && role != null;
    }

    public boolean isAdmin() {
        return role == UserRole.ROLE_SYSTEM_ADMIN;
    }

    public boolean isAirlineOwner() {
        return role == UserRole.ROLE_AIRLINE_OWNER;
    }

    public boolean isCustomer() {
        return role == UserRole.ROLE_CUSTOMER;
    }

    /** System admins and airline owners: the people who manage airline data. */
    public boolean isStaff() {
        return isAdmin() || isAirlineOwner();
    }

    public boolean hasAnyRole(UserRole... roles) {
        return role != null && Arrays.asList(roles).contains(role);
    }
}
