package com.rms.iam.repository;

import com.rms.iam.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    Optional<PasswordResetToken> findByTokenHashAndUsedFalseAndExpiresAtAfter(String tokenHash, java.time.Instant now);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE PasswordResetToken p SET p.used = true WHERE p.user.id = :userId")
    void invalidateAllByUserId(UUID userId);
}
