package com.rms.ats.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rms.ats.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
}
