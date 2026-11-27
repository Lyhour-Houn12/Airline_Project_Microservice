package com.jing.user.service;

import com.jing.common.enumeration.UserRole;
import com.jing.common.dto.user.UserDto;
import com.jing.common.pagination.PageResponse;
import com.jing.user.payload.request.ChangePasswordRequest;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserDto getById(Long id);

    PageResponse<UserDto> getAll(Pageable pageable);

    void changePassword(Long userId, ChangePasswordRequest request);

    UserDto updateRole(Long adminId, Long userId, UserRole role);
}
