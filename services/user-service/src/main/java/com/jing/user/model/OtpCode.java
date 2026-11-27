package com.jing.user.model;

import com.jing.common.enumeration.OtpPurpose;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "otp_codes", indexes = @Index(name = "idx_otp_user_purpose", columnList = "user_id, purpose"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OtpPurpose purpose;

    @Column(nullable = false)
    private String codeHash;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    @Builder.Default
    @Column(nullable = false)
    private int attempts = 0;

    @Builder.Default
    @Column(nullable = false)
    private boolean consumed = false;
}
