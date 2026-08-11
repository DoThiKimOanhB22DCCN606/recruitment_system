package com.rms.iam.repository;

import com.rms.iam.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {
    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);
    
    Optional<EmailVerificationToken> findByTokenHashAndUsedFalseAndExpiresAtAfter(String tokenHash, java.time.Instant now);

    @Modifying
    @Query("UPDATE EmailVerificationToken e SET e.used = true WHERE e.user.id = :userId")
    void invalidateAllByUserId(UUID userId);
}
