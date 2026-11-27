package com.jing.common.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Optional marker for a {@link CurrentUser} controller parameter.
 *
 * <p>A plain {@code CurrentUser} parameter is required: a request without identity headers gets a 401.
 * Use {@code @AuthUser(required = false) CurrentUser user} on public endpoints; anonymous callers then
 * receive {@link CurrentUser#ANONYMOUS}.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuthUser {

    boolean required() default true;
}
