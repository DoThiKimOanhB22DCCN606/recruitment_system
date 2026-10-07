package com.recruitment.job.dto.response;

import com.recruitment.job.entity.Job;
import com.recruitment.job.entity.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobResponse {
    private Long id;
    private Long companyId;
    private String companyName;
    private Long categoryId;
    private String categoryName;
    private String title;
    private String description;
    private String requirements;
    private String benefits;
    private String location;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String salaryCurrency;
    private Job.EmploymentType employmentType;
    private Job.ExperienceLevel experienceLevel;
    private boolean remote;
    private Integer vacancies;
    private JobStatus status;
    private LocalDateTime publishedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime closedAt;
    private List<String> skills;
    private List<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
