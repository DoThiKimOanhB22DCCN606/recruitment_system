package com.rms.ats.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import com.rms.ats.entity.Application;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {
    boolean existsByJobIdAndCandidateId(UUID jobId, UUID candidateId);
    boolean existsByCvIdAndStatus(UUID cvId, com.rms.ats.entity.ApplicationStatus status);

    @EntityGraph(attributePaths = "currentStage")
    Optional<Application> findByIdAndTenantId(UUID id, UUID tenantId);

    @EntityGraph(attributePaths = "currentStage")
    Optional<Application> findOneById(UUID id);

    @EntityGraph(attributePaths = "currentStage")
    Page<Application> findByJobIdAndTenantIdAndCurrentStageIdOrderByAppliedAtDesc(
            UUID jobId, UUID tenantId, UUID currentStageId, Pageable pageable);

    @EntityGraph(attributePaths = "currentStage")
    Page<Application> findByCandidateIdOrderByAppliedAtDesc(UUID candidateId, Pageable pageable);

    @Query("select a.id from Application a where a.candidateId = :candidateId")
    java.util.List<UUID> findApplicationIdsByCandidateId(@Param("candidateId") UUID candidateId);
}
