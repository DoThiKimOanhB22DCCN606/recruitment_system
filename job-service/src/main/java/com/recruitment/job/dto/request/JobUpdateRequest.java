package com.recruitment.job.dto.request;

import com.recruitment.job.entity.Job;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class JobUpdateRequest {

    @Size(max = 255)
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
    private Boolean remote;

    @Min(1)
    private Integer vacancies;

    private Long categoryId;
    private List<String> skills;
    private List<String> tags;
    private Integer expiryDays;
}
