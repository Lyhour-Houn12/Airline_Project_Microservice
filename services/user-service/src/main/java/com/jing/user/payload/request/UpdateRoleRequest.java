package com.jing.user.payload.request;

import com.jing.common.enumeration.UserRole;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(@NotNull UserRole role) {
}
