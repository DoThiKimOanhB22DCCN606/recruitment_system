package com.rms.ats.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rms.ats.entity.ApplicationStageHistory;

public interface ApplicationStageHistoryRepository extends JpaRepository<ApplicationStageHistory, UUID> {
    List<ApplicationStageHistory> findByApplicationIdOrderByCreatedAtAsc(UUID applicationId);
}
