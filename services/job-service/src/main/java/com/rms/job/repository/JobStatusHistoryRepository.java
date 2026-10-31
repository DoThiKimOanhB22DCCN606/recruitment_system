package com.rms.job.repository;

import com.rms.job.entity.JobStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobStatusHistoryRepository extends JpaRepository<JobStatusHistory, Long> {
    List<JobStatusHistory> findByJobIdOrderByChangedAtDesc(Long jobId);
}
