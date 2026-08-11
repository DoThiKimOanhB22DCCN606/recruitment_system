package com.recruitment.ats.domain;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue
    private UUID id;
    private UUID tenantId;
    private UUID actorId;
    private String actionType;
    private String entityType;
    private UUID entityId;
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode beforeState;
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode afterState;
    private String ipAddress;
    private String deviceInfo;
    private Instant createdAt = Instant.now();
}
