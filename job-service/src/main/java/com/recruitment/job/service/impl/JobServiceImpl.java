package com.recruitment.job.service.impl;

import com.recruitment.job.dto.request.*;
import com.recruitment.job.dto.response.*;
import com.recruitment.job.entity.*;
import com.recruitment.job.exception.InvalidStatusTransitionException;
import com.recruitment.job.exception.ResourceNotFoundException;
import com.recruitment.job.repository.JobCategoryRepository;
import com.recruitment.job.repository.JobRepository;
import com.recruitment.job.repository.JobStatusHistoryRepository;
import com.recruitment.job.service.JobMapper;
import com.recruitment.job.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final JobCategoryRepository jobCategoryRepository;
    private final JobStatusHistoryRepository historyRepository;
    private final JobMapper jobMapper;

    @Override
    public JobResponse createJob(JobCreateRequest request, Long employerId) {
        Job job = Job.builder()
                .companyId(employerId) // In production this would be resolved from the employer's company membership
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
                .remote(request.isRemote())
                .vacancies(request.getVacancies())
                .status(JobStatus.DRAFT)
                .createdBy(employerId)
                .build();

        if (request.getCategoryId() != null) {
            JobCategory category = jobCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.getCategoryId()));
            job.setCategory(category);
        }

        if (request.getSkills() != null) {
            request.getSkills().forEach(job::addSkill);
        }
        if (request.getTags() != null) {
            request.getTags().forEach(job::addTag);
        }

        Job saved = jobRepository.save(job);
        recordHistory(saved.getId(), null, JobStatus.DRAFT, employerId, "Job created");
        return jobMapper.toResponse(saved);
    }

    @Override
    public JobResponse updateJob(Long jobId, JobUpdateRequest request, Long employerId) {
        Job job = getJobOrThrow(jobId);

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
        if (request.getRemote() != null) job.setRemote(request.getRemote());
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

        return jobMapper.toResponse(jobRepository.save(job));
    }

    @Override
    public void deleteJob(Long jobId, Long employerId) {
        Job job = getJobOrThrow(jobId);
        jobRepository.delete(job);
    }

    @Override
    @Transactional(readOnly = true)
    public JobResponse getJobById(Long jobId) {
        return jobMapper.toResponse(getJobOrThrow(jobId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobSummaryResponse> listJobs(Long companyId, Pageable pageable) {
        Page<Job> jobs = (companyId != null)
                ? new org.springframework.data.domain.PageImpl<>(jobRepository.findByCompanyId(companyId))
                : jobRepository.findAll(pageable);
        return jobs.map(jobMapper::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobSummaryResponse> search(JobSearchRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        Page<Job> results = jobRepository.search(
                blankToNull(request.getKeyword()),
                blankToNull(request.getLocation()),
                blankToNull(request.getEmploymentType()),
                blankToNull(request.getExperienceLevel()),
                request.getRemote(),
                request.getCategoryId(),
                request.getSalaryMin(),
                request.getSalaryMax(),
                pageable
        );
        return results.map(jobMapper::toSummary);
    }

    @Override
    public JobResponse changeStatus(Long jobId, JobStatusChangeRequest request, Long actorId) {
        Job job = getJobOrThrow(jobId);
        JobStatus current = job.getStatus();
        JobStatus target = request.getTargetStatus();

        if (!current.canTransitionTo(target)) {
            throw new InvalidStatusTransitionException(
                    "Cannot transition job %d from %s to %s".formatted(jobId, current, target));
        }

        job.setStatus(target);
        LocalDateTime now = LocalDateTime.now();

        switch (target) {
            case PUBLISHED -> {
                job.setPublishedAt(now);
                // expiresAt might already be set on approval; default to +30 days if absent
                if (job.getExpiresAt() == null) {
                    job.setExpiresAt(now.plusDays(30));
                }
            }
            case CLOSED -> job.setClosedAt(now);
            default -> { /* no side effects */ }
        }

        Job saved = jobRepository.save(job);
        recordHistory(jobId, current, target, actorId, request.getNote());
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
        Job job = getJobOrThrow(jobId);
        return JobInternalStatusResponse.builder()
                .jobId(job.getId())
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
            jobRepository.save(job);
            recordHistory(job.getId(), previous, JobStatus.CLOSED, null, "Auto-closed: expiry date reached");
        }
        return expired.size();
    }

    private Job getJobOrThrow(Long jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));
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
