package com.jing.user.service.impl;

import com.jing.common.enumeration.UserRole;
import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.ResourceNotFoundException;
import com.jing.common.dto.user.UserDto;
import com.jing.common.pagination.PageResponse;
import com.jing.user.mapper.UserMapper;
import com.jing.user.model.User;
import com.jing.user.payload.request.ChangePasswordRequest;
import com.jing.user.repository.UserRepository;
import com.jing.user.service.RefreshTokenService;
import com.jing.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDto getById(Long id) {
        return UserMapper.toDto(findOrThrow(id));
    }

    @Override
    public PageResponse<UserDto> getAll(Pageable pageable) {
        return PageResponse.from(
                userRepository.findAll(pageable).map(UserMapper::toDto)
        );
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findOrThrow(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect.");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        refreshTokenService.revokeAll(userId);
    }

    @Override
    @Transactional
    public UserDto updateRole(Long adminId, Long userId, UserRole role) {
        if (adminId.equals(userId)) {
            throw new BadRequestException("You cannot change your own role.");
        }
        User user = findOrThrow(userId);
        user.setRole(role);
        refreshTokenService.revokeAll(userId);
        return UserMapper.toDto(user);
    }

    private User findOrThrow(Long userId){
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }
}
