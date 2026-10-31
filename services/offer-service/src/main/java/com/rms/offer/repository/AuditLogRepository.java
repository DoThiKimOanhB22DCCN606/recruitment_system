package com.rms.offer.repository;

import com.rms.offer.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByOfferIdAndTenantIdOrderByCreatedAtDesc(UUID offerId, UUID tenantId);
}
