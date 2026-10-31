package com.rms.iam.service;

import com.rms.iam.entity.AuditLog;
import com.rms.iam.repository.AuditLogRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public Page<AuditLog> searchLogs(UUID tenantId, UUID actorId, String entityId, String actionType, Instant from, Instant to, Pageable pageable) {
        Specification<AuditLog> spec = buildSpecification(tenantId, actorId, entityId, actionType, from, to);
        return auditLogRepository.findAll(spec, pageable);
    }

    public List<AuditLog> exportLogs(UUID tenantId, UUID actorId, String entityId, String actionType, Instant from, Instant to) {
        Specification<AuditLog> spec = buildSpecification(tenantId, actorId, entityId, actionType, from, to);
        return auditLogRepository.findAll(spec);
    }

    public String toCsv(List<AuditLog> logs) {
        StringBuilder sb = new StringBuilder();
        sb.append("id,tenant_id,actor_id,action_type,entity_type,entity_id,ip_address,created_at\n");
        for (AuditLog log : logs) {
            sb.append(escapeCsv(log.getId() != null ? log.getId().toString() : "")).append(",")
              .append(escapeCsv(log.getTenantId() != null ? log.getTenantId().toString() : "")).append(",")
              .append(escapeCsv(log.getActorId() != null ? log.getActorId().toString() : "")).append(",")
              .append(escapeCsv(log.getActionType())).append(",")
              .append(escapeCsv(log.getEntityType())).append(",")
              .append(escapeCsv(log.getEntityId())).append(",")
              .append(escapeCsv(log.getIpAddress())).append(",")
              .append(escapeCsv(log.getCreatedAt() != null ? log.getCreatedAt().toString() : "")).append("\n");
        }
        return sb.toString();
    }

    private Specification<AuditLog> buildSpecification(UUID tenantId, UUID actorId, String entityId, String actionType, Instant from, Instant to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (tenantId != null) predicates.add(cb.equal(root.get("tenantId"), tenantId));
            if (actorId != null) predicates.add(cb.equal(root.get("actorId"), actorId));
            if (entityId != null && !entityId.isBlank()) predicates.add(cb.equal(root.get("entityId"), entityId));
            if (actionType != null && !actionType.isBlank()) predicates.add(cb.equal(root.get("actionType"), actionType));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
