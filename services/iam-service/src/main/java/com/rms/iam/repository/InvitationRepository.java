package com.rms.iam.repository;

import com.rms.iam.entity.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, UUID> {
    Optional<Invitation> findByTokenHash(String tokenHash);
    List<Invitation> findByTenantIdAndAcceptedFalse(UUID tenantId);
}
