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
                .remote(job.isRemote())
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
                .title(job.getTitle())
                .companyName(job.getCompanyName())
                .location(job.getLocation())
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .salaryCurrency(job.getSalaryCurrency())
                .employmentType(job.getEmploymentType())
                .remote(job.isRemote())
                .status(job.getStatus())
                .skills(job.getSkills().stream().map(JobSkill::getSkill).toList())
                .publishedAt(job.getPublishedAt())
                .build();
    }

    public List<JobSummaryResponse> toSummaryList(List<Job> jobs) {
        return jobs.stream().map(this::toSummary).toList();
    }
}
