package com.recruitment.job.service.impl;

import com.recruitment.job.dto.request.*;
import com.recruitment.job.dto.response.*;
import com.recruitment.job.entity.*;
import com.recruitment.job.exception.InvalidStatusTransitionException;
import com.recruitment.job.exception.ResourceNotFoundException;
import com.recruitment.job.messaging.JobEventPublisher;
import com.recruitment.job.repository.JobCategoryRepository;
import com.recruitment.job.repository.JobRepository;
import com.recruitment.job.repository.JobStatusHistoryRepository;
import com.recruitment.job.service.JobMapper;
import com.recruitment.job.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final JobCategoryRepository jobCategoryRepository;
    private final JobStatusHistoryRepository historyRepository;
    private final com.recruitment.job.repository.AuditLogRepository auditLogRepository;
    private final com.recruitment.job.repository.search.JobSearchRepository jobSearchRepository;
    private final JobMapper jobMapper;
    private final JobEventPublisher jobEventPublisher;
    private final com.recruitment.job.client.AtsClient atsClient;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Override
    public JobResponse createJob(JobCreateRequest request, UUID userId, UUID tenantId, String role) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID is required to create a job");
        }

        // Validate salary min <= salary max
        if (request.getSalaryMin() != null && request.getSalaryMax() != null) {
            if (request.getSalaryMin().compareTo(request.getSalaryMax()) > 0) {
                throw new IllegalArgumentException("Minimum salary cannot exceed maximum salary");
            }
        }

        // Role-based initial status
        JobStatus initialStatus;
        boolean isDraft = Boolean.TRUE.equals(request.getDraft());
        if (isDraft) {
            initialStatus = JobStatus.DRAFT;
        } else if ("HR_ADMIN".equalsIgnoreCase(role)) {
            initialStatus = JobStatus.PUBLISHED;
        } else {
            initialStatus = JobStatus.PENDING_APPROVAL;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime publishedAt = (initialStatus == JobStatus.PUBLISHED) ? now : null;
        LocalDateTime expiresAt = (initialStatus == JobStatus.PUBLISHED)
                ? now.plusDays(request.getExpiryDays() != null ? request.getExpiryDays() : 30)
                : null;

        Long createdByLong = userId != null ? Math.abs(userId.getMostSignificantBits()) : 1L;

        JobCategory category = null;
        if (request.getCategoryId() != null) {
            category = jobCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.getCategoryId()));
        }

        Job job = Job.builder()
                .tenantId(tenantId)
                .recruiterId(userId)
                .companyId(createdByLong)
                .category(category)
                .title(request.getTitle())
                .description(request.getDescription())
                .requirements(request.getRequirements())
                .benefits(request.getBenefits())
                .location(request.getLocation())
                .salaryMin(request.getSalaryMin())
                .salaryMax(request.getSalaryMax())
                .salaryCurrency(request.getSalaryCurrency() != null ? request.getSalaryCurrency() : "VND")
                .employmentType(request.getEmploymentType())
                .experienceLevel(request.getExperienceLevel())
                .workType(request.getWorkType() != null ? request.getWorkType() : Job.WorkType.ONSITE)
                .vacancies(request.getVacancies() != null ? request.getVacancies() : 1)
                .status(initialStatus)
                .publishedAt(publishedAt)
                .expiresAt(expiresAt)
                .createdBy(createdByLong)
                .build();

        if (request.getSkills() != null) {
            request.getSkills().forEach(job::addSkill);
        }
        if (request.getTags() != null) {
            request.getTags().forEach(job::addTag);
        }

        Job saved = jobRepository.save(job);
        syncToElasticsearch(saved);
        recordHistory(saved.getId(), null, initialStatus, createdByLong, "Job created with initial status: " + initialStatus);

        // Initialize default pipeline stages in ATS
        atsClient.initializeDefaultStages(saved.getId(), tenantId);

        // Publish job.created event
        jobEventPublisher.publishJobCreated(saved.getId(), tenantId, saved.getTitle(), userId);

        return jobMapper.toResponse(saved);
    }

    @Override
    public JobResponse updateJob(Long jobId, JobUpdateRequest request, UUID userId, UUID tenantId, String role) {
        Job job = getJobForTenantOrThrow(jobId, tenantId);

        java.math.BigDecimal effectiveSalaryMin = request.getSalaryMin() != null ? request.getSalaryMin() : job.getSalaryMin();
        java.math.BigDecimal effectiveSalaryMax = request.getSalaryMax() != null ? request.getSalaryMax() : job.getSalaryMax();
        if (effectiveSalaryMin != null && effectiveSalaryMax != null) {
            if (effectiveSalaryMin.compareTo(effectiveSalaryMax) > 0) {
                throw new IllegalArgumentException("Minimum salary cannot exceed maximum salary");
            }
        }

        boolean isSignificantEdit = false;
        if (job.getStatus() == JobStatus.PUBLISHED) {
            if (request.getTitle() != null && !request.getTitle().equals(job.getTitle())) isSignificantEdit = true;
            if (request.getLocation() != null && !request.getLocation().equals(job.getLocation())) isSignificantEdit = true;
            if (request.getSalaryMin() != null && !request.getSalaryMin().equals(job.getSalaryMin())) isSignificantEdit = true;
            if (request.getSalaryMax() != null && !request.getSalaryMax().equals(job.getSalaryMax())) isSignificantEdit = true;
            if (request.getSalaryCurrency() != null && !request.getSalaryCurrency().equals(job.getSalaryCurrency())) isSignificantEdit = true;
            if (request.getEmploymentType() != null && request.getEmploymentType() != job.getEmploymentType()) isSignificantEdit = true;
            if (request.getWorkType() != null && request.getWorkType() != job.getWorkType()) isSignificantEdit = true;
        }

        String beforeStateJson = null;
        if (isSignificantEdit) {
            java.util.Map<String, Object> beforeMap = new java.util.LinkedHashMap<>();
            beforeMap.put("title", job.getTitle());
            beforeMap.put("location", job.getLocation());
            beforeMap.put("salaryMin", job.getSalaryMin());
            beforeMap.put("salaryMax", job.getSalaryMax());
            beforeMap.put("salaryCurrency", job.getSalaryCurrency());
            beforeMap.put("employmentType", job.getEmploymentType() != null ? job.getEmploymentType().name() : null);
            beforeMap.put("workType", job.getWorkType() != null ? job.getWorkType().name() : null);
            try {
                beforeStateJson = objectMapper.writeValueAsString(beforeMap);
            } catch (Exception e) {
                log.warn("Failed to serialize beforeState", e);
            }
        }

        if (request.getTitle() != null) job.setTitle(request.getTitle());
        if (request.getDescription() != null) job.setDescription(request.getDescription());
        if (request.getRequirements() != null) job.setRequirements(request.getRequirements());
        if (request.getBenefits() != null) job.setBenefits(request.getBenefits());
        if (request.getLocation() != null) job.setLocation(request.getLocation());
        if (request.getSalaryMin() != null) job.setSalaryMin(request.getSalaryMin());
        if (request.getSalaryMax() != null) job.setSalaryMax(request.getSalaryMax());
        if (request.getSalaryCurrency() != null) job.setSalaryCurrency(request.getSalaryCurrency());
        if (request.getEmploymentType() != null) job.setEmploymentType(request.getEmploymentType());
        if (request.getExperienceLevel() != null) job.setExperienceLevel(request.getExperienceLevel());
        if (request.getWorkType() != null) job.setWorkType(request.getWorkType());
        if (request.getVacancies() != null) job.setVacancies(request.getVacancies());

        if (request.getCategoryId() != null) {
            JobCategory category = jobCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.getCategoryId()));
            job.setCategory(category);
        }

        if (request.getSkills() != null) {
            job.getSkills().clear();
            request.getSkills().forEach(job::addSkill);
        }
        if (request.getTags() != null) {
            job.getTags().clear();
            request.getTags().forEach(job::addTag);
        }

        Long actorId = userId != null ? Math.abs(userId.getMostSignificantBits()) : null;
        recordHistory(jobId, job.getStatus(), job.getStatus(), actorId, "Job details updated");

        if (isSignificantEdit && tenantId != null) {
            String afterStateJson = null;
            java.util.Map<String, Object> afterMap = new java.util.LinkedHashMap<>();
            afterMap.put("title", job.getTitle());
            afterMap.put("location", job.getLocation());
            afterMap.put("salaryMin", job.getSalaryMin());
            afterMap.put("salaryMax", job.getSalaryMax());
            afterMap.put("salaryCurrency", job.getSalaryCurrency());
            afterMap.put("employmentType", job.getEmploymentType() != null ? job.getEmploymentType().name() : null);
            afterMap.put("workType", job.getWorkType() != null ? job.getWorkType().name() : null);
            try {
                afterStateJson = objectMapper.writeValueAsString(afterMap);
            } catch (Exception e) {
                log.warn("Failed to serialize afterState", e);
            }

            com.recruitment.job.entity.AuditLog audit = com.recruitment.job.entity.AuditLog.builder()
                    .tenantId(tenantId)
                    .actorId(userId)
                    .actionType("JOB_SIGNIFICANT_FIELD_EDITED")
                    .entityType("JOB")
                    .entityId(String.valueOf(jobId))
                    .beforeState(beforeStateJson)
                    .afterState(afterStateJson)
                    .createdAt(java.time.Instant.now())
                    .build();
            auditLogRepository.save(audit);
        }

        Job saved = jobRepository.save(job);
        syncToElasticsearch(saved);
        return jobMapper.toResponse(saved);
    }

    @Override
    public void deleteJob(Long jobId, UUID userId, UUID tenantId, String role) {
        Job job = getJobForTenantOrThrow(jobId, tenantId);
        jobRepository.delete(job);
        try {
            jobSearchRepository.deleteById(String.valueOf(jobId));
        } catch (Exception e) {
            log.warn("Elasticsearch delete failed for job {}: {}", jobId, e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public JobResponse getJobById(Long jobId, UUID tenantId, String role) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        // Multi-tenant privacy rule:
        // Guests/Candidates can only see PUBLISHED jobs.
        // Employers can see their own tenant's jobs in any status.
        if (tenantId != null && tenantId.equals(job.getTenantId())) {
            return jobMapper.toResponse(job);
        }

        if (job.getStatus() != JobStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Job not found or not published: " + jobId);
        }

        return jobMapper.toResponse(job);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobSummaryResponse> listJobs(UUID tenantId, String role, Long companyId, Pageable pageable) {
        if (tenantId != null && ("HR_ADMIN".equalsIgnoreCase(role) || "RECRUITER".equalsIgnoreCase(role))) {
            // Employer view: list all jobs for the tenant
            return jobRepository.findByTenantId(tenantId, pageable).map(jobMapper::toSummary);
        }

        // Public/Candidate view: list only PUBLISHED jobs
        return jobRepository.findByStatus(JobStatus.PUBLISHED, pageable).map(jobMapper::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobSummaryResponse> search(JobSearchRequest request) {
        Page<com.recruitment.job.document.JobDocument> results = jobSearchRepository.search(request);
        return results.map(jobMapper::toSummary);
    }

    @Override
    public JobResponse changeStatus(Long jobId, JobStatusChangeRequest request, UUID userId, UUID tenantId, String role) {
        Job job = getJobForTenantOrThrow(jobId, tenantId);
        JobStatus current = job.getStatus();
        JobStatus target = request.getTargetStatus();

        if (!current.canTransitionTo(target)) {
            throw new InvalidStatusTransitionException(
                    "Cannot transition job %d from %s to %s".formatted(jobId, current, target));
        }

        // Enforce RBAC for transitions
        if (target == JobStatus.PUBLISHED && current == JobStatus.PENDING_APPROVAL) {
            // Approval: Only HR_ADMIN allowed
            if (!"HR_ADMIN".equalsIgnoreCase(role)) {
                throw new IllegalArgumentException("Only HR_ADMIN can approve jobs");
            }
        }

        if (target == JobStatus.DRAFT && current == JobStatus.PENDING_APPROVAL) {
            // Rejection: Only HR_ADMIN allowed, note is mandatory
            if (!"HR_ADMIN".equalsIgnoreCase(role)) {
                throw new IllegalArgumentException("Only HR_ADMIN can reject jobs");
            }
            if (request.getNote() == null || request.getNote().isBlank()) {
                throw new IllegalArgumentException("Rejection reason is required when rejecting a job");
            }
        }

        job.setStatus(target);
        LocalDateTime now = LocalDateTime.now();

        if (target == JobStatus.PUBLISHED) {
            job.setPublishedAt(now);
            if (job.getExpiresAt() == null) {
                job.setExpiresAt(now.plusDays(30));
            }
        } else if (target == JobStatus.CLOSED) {
            job.setClosedAt(now);
        }

        Job saved = jobRepository.save(job);
        syncToElasticsearch(saved);
        Long actorId = userId != null ? Math.abs(userId.getMostSignificantBits()) : null;
        recordHistory(jobId, current, target, actorId, request.getNote());

        // Emit approval outcome event to notify recruiter
        if (current == JobStatus.PENDING_APPROVAL && (target == JobStatus.PUBLISHED || target == JobStatus.DRAFT)) {
            String outcomeStatus = (target == JobStatus.PUBLISHED) ? "APPROVED" : "REJECTED";
            jobEventPublisher.publishApprovalOutcome(jobId, tenantId, job.getRecruiterId(), outcomeStatus, request.getNote());
        }

        return jobMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobStatusHistoryResponse> getStatusHistory(Long jobId) {
        return historyRepository.findByJobIdOrderByChangedAtDesc(jobId).stream()
                .map(h -> JobStatusHistoryResponse.builder()
                        .id(h.getId())
                        .fromStatus(h.getFromStatus())
                        .toStatus(h.getToStatus())
                        .changedBy(h.getChangedBy())
                        .note(h.getNote())
                        .changedAt(h.getChangedAt())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public JobInternalStatusResponse getInternalStatus(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));
        return JobInternalStatusResponse.builder()
                .id(job.getId())
                .jobId(job.getId())
                .tenantId(job.getTenantId())
                .recruiterId(job.getRecruiterId())
                .status(job.getStatus())
                .acceptingApplications(job.getStatus() == JobStatus.PUBLISHED)
                .build();
    }

    @Override
    public int closeExpiredJobs() {
        List<Job> expired = jobRepository.findExpiredJobs(JobStatus.PUBLISHED, LocalDateTime.now());
        for (Job job : expired) {
            JobStatus previous = job.getStatus();
            job.setStatus(JobStatus.CLOSED);
            job.setClosedAt(LocalDateTime.now());
            Job saved = jobRepository.save(job);
            syncToElasticsearch(saved);
            recordHistory(job.getId(), previous, JobStatus.CLOSED, null, "Auto-closed: expiry date reached");
        }
        return expired.size();
    }

    private void syncToElasticsearch(Job job) {
        if (job == null || job.getId() == null) return;
        try {
            if (job.getStatus() == JobStatus.PUBLISHED) {
                jobSearchRepository.save(jobMapper.toDocument(job));
            } else {
                jobSearchRepository.deleteById(String.valueOf(job.getId()));
            }
        } catch (Exception e) {
            log.warn("Elasticsearch sync failed for job {}: {}", job.getId(), e.getMessage());
        }
    }

    private Job getJobForTenantOrThrow(Long jobId, UUID tenantId) {
        if (tenantId == null) {
            return jobRepository.findById(jobId)
                    .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));
        }
        return jobRepository.findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found for this tenant: " + jobId));
    }

    private void recordHistory(Long jobId, JobStatus from, JobStatus to, Long actorId, String note) {
        historyRepository.save(JobStatusHistory.builder()
                .jobId(jobId)
                .fromStatus(from)
                .toStatus(to)
                .changedBy(actorId)
                .note(note)
                .build());
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
