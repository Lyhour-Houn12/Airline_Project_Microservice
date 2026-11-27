package com.jing.common.security;

import com.jing.common.exception.ForbiddenException;
import com.jing.common.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import static com.jing.common.enumeration.UserRole.ROLE_AIRLINE_OWNER;
import static com.jing.common.enumeration.UserRole.ROLE_SYSTEM_ADMIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequireRoleInterceptorTest {

    private final RequireRoleInterceptor interceptor = new RequireRoleInterceptor();

    @RequireRole(ROLE_SYSTEM_ADMIN)
    static class AdminController {
        public void adminOnly() {}

        @RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
        public void staff() {}
    }

    static class PublicController {
        public void open() {}
    }

    @Test
    void allowsAnyoneWithoutAnnotation() throws Exception {
        assertThat(interceptor.preHandle(request(null, null), new MockHttpServletResponse(),
                handler(new PublicController(), "open"))).isTrue();
    }

    @Test
    void classAnnotationApplies() throws Exception {
        HandlerMethod adminOnly = handler(new AdminController(), "adminOnly");

        assertThat(interceptor.preHandle(request("1", "ROLE_SYSTEM_ADMIN"), new MockHttpServletResponse(), adminOnly))
                .isTrue();
        assertThatThrownBy(() -> interceptor.preHandle(request("1", "ROLE_AIRLINE_OWNER"),
                new MockHttpServletResponse(), adminOnly)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void methodAnnotationOverridesClass() throws Exception {
        HandlerMethod staff = handler(new AdminController(), "staff");

        assertThat(interceptor.preHandle(request("1", "ROLE_AIRLINE_OWNER"), new MockHttpServletResponse(), staff))
                .isTrue();
        assertThatThrownBy(() -> interceptor.preHandle(request("1", "ROLE_CUSTOMER"),
                new MockHttpServletResponse(), staff)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void missingHeadersGive401Not403() throws Exception {
        HandlerMethod staff = handler(new AdminController(), "staff");

        assertThatThrownBy(() -> interceptor.preHandle(request(null, null), new MockHttpServletResponse(), staff))
                .isInstanceOf(UnauthorizedException.class);
    }

    private static MockHttpServletRequest request(String id, String role) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (id != null) request.addHeader(AuthHeaders.USER_ID, id);
        if (role != null) request.addHeader(AuthHeaders.USER_ROLE, role);
        return request;
    }

    private static HandlerMethod handler(Object bean, String method) throws NoSuchMethodException {
        return new HandlerMethod(bean, bean.getClass().getMethod(method));
    }
}
