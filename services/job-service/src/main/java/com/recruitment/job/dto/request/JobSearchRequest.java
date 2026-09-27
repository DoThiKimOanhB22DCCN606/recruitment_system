package com.recruitment.job.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class JobSearchRequest {
    private String keyword;
    private String location;
    private String employmentType;
    private String experienceLevel;
    private String workType;
    private Long categoryId;
    private java.util.List<String> skills;
    private String skill;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String sortBy;
    private String sortOrder;
    private int page = 0;
    private int size = 20;
}
