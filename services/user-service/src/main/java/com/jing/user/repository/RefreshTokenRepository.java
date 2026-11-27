package com.jing.user.repository;

import com.jing.user.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE RefreshToken t SET t.revoked = true WHERE t.user.id = :userId and t.revoked = false")
    void revokeAllByUserId(@Param("userId") Long userId);
}
