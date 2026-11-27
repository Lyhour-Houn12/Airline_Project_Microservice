package com.jing.user.payload.request;

import jakarta.validation.constraints.*;

public record SignupRequest(
        @NotBlank @Size(max = 100) String fullname,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72, message = "password must be 8-72 characters") String password, // BCrypt ignores bytes after 72
        @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "phone must be 8-15 digits") String phone
        ) {
}
