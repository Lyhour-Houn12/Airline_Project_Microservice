package com.jing.common.security;

import com.jing.common.exception.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/** Enforces {@link RequireRole} before the controller method runs. */
public class RequireRoleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        RequireRole rule = AnnotatedElementUtils.findMergedAnnotation(method.getMethod(), RequireRole.class);
        if (rule == null) {
            rule = AnnotatedElementUtils.findMergedAnnotation(method.getBeanType(), RequireRole.class);
        }
        if (rule == null) {
            return true;
        }

        CurrentUser user = CurrentUserResolver.resolve(request, true);   // 401 when missing
        if (!user.hasAnyRole(rule.value())) {
            throw new ForbiddenException("You do not have permission to perform this action");
        }
        return true;
    }
}
