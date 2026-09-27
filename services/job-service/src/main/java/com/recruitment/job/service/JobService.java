package com.recruitment.job.service;

import com.recruitment.job.dto.request.*;
import com.recruitment.job.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface JobService {

    JobResponse createJob(JobCreateRequest request, UUID userId, UUID tenantId, String role);

    JobResponse updateJob(Long jobId, JobUpdateRequest request, UUID userId, UUID tenantId, String role);

    void deleteJob(Long jobId, UUID userId, UUID tenantId, String role);

    JobResponse getJobById(Long jobId, UUID tenantId, String role);

    Page<JobSummaryResponse> listJobs(UUID tenantId, String role, Long companyId, Pageable pageable);

    Page<JobSummaryResponse> search(JobSearchRequest request);

    JobResponse changeStatus(Long jobId, JobStatusChangeRequest request, UUID userId, UUID tenantId, String role);

    List<JobStatusHistoryResponse> getStatusHistory(Long jobId);

    JobInternalStatusResponse getInternalStatus(Long jobId);

    /** Invoked by scheduled task; closes every PUBLISHED job whose expiresAt has passed. */
    int closeExpiredJobs();
}
