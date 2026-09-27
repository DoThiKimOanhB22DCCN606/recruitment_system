package com.recruitment.job.service;

import com.recruitment.job.dto.response.JobResponse;
import com.recruitment.job.dto.response.JobSummaryResponse;
import com.recruitment.job.entity.Job;
import com.recruitment.job.entity.JobSkill;
import com.recruitment.job.entity.JobTag;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JobMapper {

    public JobResponse toResponse(Job job) {
        return JobResponse.builder()
                .id(job.getId())
                .tenantId(job.getTenantId())
                .recruiterId(job.getRecruiterId())
                .companyId(job.getCompanyId())
                .companyName(job.getCompanyName())
                .categoryId(job.getCategory() != null ? job.getCategory().getId() : null)
                .categoryName(job.getCategory() != null ? job.getCategory().getName() : null)
                .title(job.getTitle())
                .description(job.getDescription())
                .requirements(job.getRequirements())
                .benefits(job.getBenefits())
                .location(job.getLocation())
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .salaryCurrency(job.getSalaryCurrency())
                .employmentType(job.getEmploymentType())
                .experienceLevel(job.getExperienceLevel())
                .workType(job.getWorkType())
                .vacancies(job.getVacancies())
                .status(job.getStatus())
                .publishedAt(job.getPublishedAt())
                .expiresAt(job.getExpiresAt())
                .closedAt(job.getClosedAt())
                .skills(job.getSkills().stream().map(JobSkill::getSkill).toList())
                .tags(job.getTags().stream().map(JobTag::getTag).toList())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }

    public JobSummaryResponse toSummary(Job job) {
        return JobSummaryResponse.builder()
                .id(job.getId())
                .tenantId(job.getTenantId())
                .title(job.getTitle())
                .companyName(job.getCompanyName())
                .location(job.getLocation())
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .salaryCurrency(job.getSalaryCurrency())
                .employmentType(job.getEmploymentType())
                .workType(job.getWorkType())
                .status(job.getStatus())
                .skills(job.getSkills().stream().map(JobSkill::getSkill).toList())
                .publishedAt(job.getPublishedAt())
                .build();
    }

    public List<JobSummaryResponse> toSummaryList(List<Job> jobs) {
        return jobs.stream().map(this::toSummary).toList();
    }

    public com.recruitment.job.document.JobDocument toDocument(Job job) {
        if (job == null) return null;
        return com.recruitment.job.document.JobDocument.builder()
                .id(String.valueOf(job.getId()))
                .jobId(job.getId())
                .tenantId(job.getTenantId())
                .companyName(job.getCompanyName())
                .title(job.getTitle())
                .description(job.getDescription())
                .requirements(job.getRequirements())
                .location(job.getLocation())
                .employmentType(job.getEmploymentType() != null ? job.getEmploymentType().name() : null)
                .experienceLevel(job.getExperienceLevel() != null ? job.getExperienceLevel().name() : null)
                .workType(job.getWorkType() != null ? job.getWorkType().name() : null)
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .salaryCurrency(job.getSalaryCurrency())
                .status(job.getStatus() != null ? job.getStatus().name() : null)
                .categoryId(job.getCategory() != null ? job.getCategory().getId() : null)
                .skills(job.getSkills() != null ? job.getSkills().stream().map(JobSkill::getSkill).toList() : List.of())
                .tags(job.getTags() != null ? job.getTags().stream().map(JobTag::getTag).toList() : List.of())
                .publishedAt(job.getPublishedAt())
                .expiresAt(job.getExpiresAt())
                .build();
    }

    public JobSummaryResponse toSummary(com.recruitment.job.document.JobDocument doc) {
        if (doc == null) return null;
        return JobSummaryResponse.builder()
                .id(doc.getJobId())
                .tenantId(doc.getTenantId())
                .title(doc.getTitle())
                .companyName(doc.getCompanyName())
                .location(doc.getLocation())
                .salaryMin(doc.getSalaryMin())
                .salaryMax(doc.getSalaryMax())
                .salaryCurrency(doc.getSalaryCurrency())
                .employmentType(doc.getEmploymentType() != null ? Job.EmploymentType.valueOf(doc.getEmploymentType()) : null)
                .workType(doc.getWorkType() != null ? Job.WorkType.valueOf(doc.getWorkType()) : null)
                .status(doc.getStatus() != null ? com.recruitment.job.entity.JobStatus.valueOf(doc.getStatus()) : null)
                .skills(doc.getSkills())
                .publishedAt(doc.getPublishedAt())
                .build();
    }
}
