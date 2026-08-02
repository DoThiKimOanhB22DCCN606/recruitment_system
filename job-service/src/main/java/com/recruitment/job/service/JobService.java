package com.recruitment.job.service;

import com.recruitment.job.dto.request.*;
import com.recruitment.job.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface JobService {

    JobResponse createJob(JobCreateRequest request, Long employerId);

    JobResponse updateJob(Long jobId, JobUpdateRequest request, Long employerId);

    void deleteJob(Long jobId, Long employerId);

    JobResponse getJobById(Long jobId);

    Page<JobSummaryResponse> listJobs(Long companyId, Pageable pageable);

    Page<JobSummaryResponse> search(JobSearchRequest request);

    JobResponse changeStatus(Long jobId, JobStatusChangeRequest request, Long actorId);

    List<JobStatusHistoryResponse> getStatusHistory(Long jobId);

    JobInternalStatusResponse getInternalStatus(Long jobId);

    /** Invoked by the scheduled task; closes every PUBLISHED job whose expiresAt has passed. */
    int closeExpiredJobs();
}
