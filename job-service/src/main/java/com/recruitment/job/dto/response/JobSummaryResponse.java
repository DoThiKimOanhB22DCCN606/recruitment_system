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

/** Lightweight representation used in list/search results. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobSummaryResponse {
    private Long id;
    private String title;
    private String companyName;
    private String location;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String salaryCurrency;
    private Job.EmploymentType employmentType;
    private boolean remote;
    private JobStatus status;
    private List<String> skills;
    private LocalDateTime publishedAt;
}
