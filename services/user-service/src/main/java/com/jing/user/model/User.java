package com.jing.user.model;


import com.jing.common.enumeration.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "fullname is required")
    @Column(nullable = false, length = 100)
    private String fullname;

    @Column(nullable = false)
    private String password;

    @NotBlank(message = "Email is required")
    @Email
    @Column(nullable = false, unique = true)
    private String email;

    @Column(length = 20)
    private String phone;

    @NotNull(message = "Role is required")
    @Enumerated(EnumType.STRING)          // was ordinal: reordering the enum would corrupt data
    @Column(nullable = false, length = 50)
    private UserRole role;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder.Default
    @Column(nullable = false)
    private boolean verified = false;

    private LocalDateTime lastLogin;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "integer default 0")
    private int failedLoginAttempts = 0;

    /** While in the future, sign-in is refused (even with the right password). */
    private LocalDateTime lockedUntil;
}
