package com.recruitment.job.dto.request;

import com.recruitment.job.entity.Job;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class JobCreateRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255)
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    private String requirements;
    private String benefits;

    @NotBlank(message = "Location is required")
    private String location;

    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String salaryCurrency;

    @NotNull(message = "Employment type is required")
    private Job.EmploymentType employmentType;

    private Job.ExperienceLevel experienceLevel;

    private boolean remote;

    @Min(1)
    private Integer vacancies = 1;

    private Long categoryId;

    private List<String> skills;
    private List<String> tags;

    /** Number of days from publish date until the job auto-closes. */
    @Positive
    private Integer expiryDays;
}
