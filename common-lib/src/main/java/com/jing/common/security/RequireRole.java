package com.jing.common.security;

import com.jing.common.enumeration.UserRole;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Restricts a controller method (or every method of a controller class) to the given roles.
 *
 * <pre>{@code
 * @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
 * @PostMapping
 * public FareResponse create(@Valid @RequestBody FareRequest request) { ... }
 * }</pre>
 *
 * Missing or invalid identity headers give a 401, a role outside the list gives a 403.
 * An annotation on a method replaces the one on its class.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {

    UserRole[] value();
}
