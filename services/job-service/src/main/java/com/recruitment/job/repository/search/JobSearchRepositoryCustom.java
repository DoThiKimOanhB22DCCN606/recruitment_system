package com.recruitment.job.repository.search;

import com.recruitment.job.document.JobDocument;
import com.recruitment.job.dto.request.JobSearchRequest;
import org.springframework.data.domain.Page;

public interface JobSearchRepositoryCustom {
    Page<JobDocument> search(JobSearchRequest request);
}