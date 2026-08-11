package com.recruitment.interview.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.recruitment.interview.domain.Interview;

public interface InterviewRepository extends JpaRepository<Interview, UUID> {
    Optional<Interview> findByIdAndTenantId(UUID id, UUID tenantId);
    List<Interview> findByApplicationIdAndTenantIdOrderByScheduledDateAscStartTimeAsc(
            UUID applicationId, UUID tenantId);
}
