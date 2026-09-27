package com.recruitment.ats.service;

import com.recruitment.ats.domain.AuditLog;
import com.recruitment.ats.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logCrossTenantAttempt(UUID tenantId, UUID actorId, UUID applicationId) {
        AuditLog audit = new AuditLog();
        audit.setId(UUID.randomUUID());
        audit.setTenantId(tenantId);
        audit.setActorId(actorId != null ? actorId : UUID.fromString("00000000-0000-0000-0000-000000000000"));
        audit.setActionType("CROSS_TENANT_ATTEMPT");
        audit.setEntityType("APPLICATION");
        audit.setEntityId(applicationId);
        audit.setCreatedAt(Instant.now());
        auditLogRepository.save(audit);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog save(AuditLog audit) {
        if (audit.getId() == null) {
            audit.setId(UUID.randomUUID());
        }
        if (audit.getCreatedAt() == null) {
            audit.setCreatedAt(Instant.now());
        }
        return auditLogRepository.save(audit);
    }
}