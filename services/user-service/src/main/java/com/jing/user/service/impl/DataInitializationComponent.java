package com.jing.user.service.impl;

import com.jing.common.enumeration.UserRole;
import com.jing.user.model.User;
import com.jing.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class DataInitializationComponent implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (adminEmail.isBlank() && adminPassword.isBlank()) {
            return;
        }
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            throw new IllegalStateException("Set both ADMIN_EMAIL and ADMIN_PASSWORD to provision the initial admin.");
        }
        initializeAdminUser();
    }

    private void initializeAdminUser() {
        String normalizedEmail = adminEmail.trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            return;
        }

        User adminUser = User.builder()
                .fullname("System Admin")
                .email(normalizedEmail)
                .password(passwordEncoder.encode(adminPassword))
                .verified(true)
                .role(UserRole.ROLE_SYSTEM_ADMIN)
                .build();
        userRepository.save(adminUser);
    }
}
