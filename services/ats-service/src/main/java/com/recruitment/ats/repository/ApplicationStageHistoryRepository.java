package com.recruitment.ats.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.recruitment.ats.domain.ApplicationStageHistory;

public interface ApplicationStageHistoryRepository extends JpaRepository<ApplicationStageHistory, UUID> {
    List<ApplicationStageHistory> findByApplicationIdOrderByCreatedAtAsc(UUID applicationId);
}
