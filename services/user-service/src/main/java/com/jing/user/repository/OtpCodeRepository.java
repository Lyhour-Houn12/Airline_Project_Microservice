package com.jing.user.repository;

import com.jing.common.enumeration.OtpPurpose;
import com.jing.user.model.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    Optional<OtpCode> findFirstByUserIdAndPurposeOrderByCreatedAtDesc(Long userId, OtpPurpose purpose);


    @Modifying(flushAutomatically = true)
    @Query("UPDATE OtpCode o set o.consumed = true where o.user.id = :userId and o.purpose = :purpose and o.consumed = false")
    void consumeAll(Long userId, OtpPurpose purpose);

    Optional<OtpCode> findFirstByUserIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(Long id, OtpPurpose purpose);
}
