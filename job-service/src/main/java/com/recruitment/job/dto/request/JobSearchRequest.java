package com.recruitment.job.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class JobSearchRequest {
    private String keyword;
    private String location;
    private String employmentType;
    private String experienceLevel;
    private Boolean remote;
    private Long categoryId;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private int page = 0;
    private int size = 20;
}
