package com.jing.common.dto.user;


import com.jing.common.enumeration.UserRole;

import java.time.LocalDateTime;


public record UserDto(
        Long id,
        String fullname,
        String email,
        String phone,
        UserRole role,
        boolean verified,
        LocalDateTime lastLogin) {
}
