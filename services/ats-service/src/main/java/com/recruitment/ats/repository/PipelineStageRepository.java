package com.recruitment.ats.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.recruitment.ats.domain.PipelineStage;

public interface PipelineStageRepository extends JpaRepository<PipelineStage, UUID> {
    List<PipelineStage> findByJobIdAndTenantIdOrderBySequenceOrder(UUID jobId, UUID tenantId);
    Optional<PipelineStage> findFirstByJobIdAndTenantIdOrderBySequenceOrder(UUID jobId, UUID tenantId);
    Optional<PipelineStage> findByIdAndJobIdAndTenantId(UUID id, UUID jobId, UUID tenantId);
    Optional<PipelineStage> findFirstByJobIdAndTenantIdAndTerminalStatus(UUID jobId, UUID tenantId, com.recruitment.ats.domain.ApplicationStatus terminalStatus);
    Optional<PipelineStage> findFirstByJobIdAndTenantIdAndNameIgnoreCase(UUID jobId, UUID tenantId, String name);
}
