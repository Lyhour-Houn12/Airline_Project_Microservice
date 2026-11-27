package com.jing.user.mapper;

import com.jing.common.dto.user.UserDto;
import com.jing.user.model.User;

import java.util.List;

public class UserMapper {

    public static UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getFullname(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.isVerified(),
                user.getLastLogin());
    }

    public static List<UserDto> toDtoList(List<User> users){
        return users.stream()
                .map(UserMapper::toDto)
                .toList();
    }
}

