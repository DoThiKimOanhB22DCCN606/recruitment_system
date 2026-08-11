package com.recruitment.ats.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.recruitment.ats.domain.Application;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {
    boolean existsByJobIdAndCandidateId(UUID jobId, UUID candidateId);

    @EntityGraph(attributePaths = "currentStage")
    Optional<Application> findByIdAndTenantId(UUID id, UUID tenantId);

    @EntityGraph(attributePaths = "currentStage")
    Optional<Application> findOneById(UUID id);

    @EntityGraph(attributePaths = "currentStage")
    Page<Application> findByJobIdAndTenantIdAndCurrentStageIdOrderByAppliedAtDesc(
            UUID jobId, UUID tenantId, UUID currentStageId, Pageable pageable);

    @EntityGraph(attributePaths = "currentStage")
    Page<Application> findByCandidateIdOrderByAppliedAtDesc(UUID candidateId, Pageable pageable);
}
