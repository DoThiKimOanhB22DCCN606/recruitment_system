package com.rms.interview.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rms.interview.entity.Interview;

public interface InterviewRepository extends JpaRepository<Interview, UUID> {
    Optional<Interview> findByIdAndTenantId(UUID id, UUID tenantId);
    List<Interview> findByTenantIdOrderByScheduledDateAscStartTimeAsc(UUID tenantId);
    List<Interview> findByApplicationIdAndTenantIdOrderByScheduledDateAscStartTimeAsc(
            UUID applicationId, UUID tenantId);
    List<Interview> findByApplicationIdInOrderByScheduledDateAscStartTimeAsc(Iterable<UUID> applicationIds);
}
