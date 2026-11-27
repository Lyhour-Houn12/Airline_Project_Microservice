package com.jing.common.security;

import com.jing.common.enumeration.UserRole;
import com.jing.common.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentUserResolverTest {

    @Test
    void readsValidHeaders() {
        CurrentUser user = CurrentUserResolver.parse("42", "ROLE_AIRLINE_OWNER");

        assertThat(user.id()).isEqualTo(42L);
        assertThat(user.role()).isEqualTo(UserRole.ROLE_AIRLINE_OWNER);
        assertThat(user.isStaff()).isTrue();
        assertThat(user.isAdmin()).isFalse();
    }

    @Test
    void noHeadersMeansAnonymous() {
        assertThat(CurrentUserResolver.parse(null, null)).isEqualTo(CurrentUser.ANONYMOUS);
        assertThat(CurrentUser.ANONYMOUS.isAuthenticated()).isFalse();
        assertThat(CurrentUser.ANONYMOUS.isStaff()).isFalse();
    }

    @Test
    void rejectsHalfAnIdentity() {
        assertThatThrownBy(() -> CurrentUserResolver.parse("42", null))
                .isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> CurrentUserResolver.parse(null, "ROLE_CUSTOMER"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void rejectsMalformedValues() {
        assertThatThrownBy(() -> CurrentUserResolver.parse("abc", "ROLE_CUSTOMER"))
                .isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> CurrentUserResolver.parse("0", "ROLE_CUSTOMER"))
                .isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> CurrentUserResolver.parse("42", "ROLE_SUPERUSER"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void requiredUserNeedsHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThatThrownBy(() -> CurrentUserResolver.resolve(request, true))
                .isInstanceOf(UnauthorizedException.class);
        assertThat(CurrentUserResolver.resolve(request, false)).isEqualTo(CurrentUser.ANONYMOUS);
    }
}
