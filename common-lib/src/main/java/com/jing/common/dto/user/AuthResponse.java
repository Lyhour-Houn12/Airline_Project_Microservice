package com.jing.common.dto.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jing.common.dto.user.UserDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private String jwt;
    private String message;
    private String title;
    private UserDto user;
    private String refreshToken;

}
