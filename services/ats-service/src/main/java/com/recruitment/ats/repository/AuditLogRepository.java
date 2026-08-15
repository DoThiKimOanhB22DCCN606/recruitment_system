package com.recruitment.ats.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.recruitment.ats.domain.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
}
