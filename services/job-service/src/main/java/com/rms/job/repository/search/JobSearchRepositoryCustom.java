package com.rms.job.repository.search;

import com.rms.job.document.JobDocument;
import com.rms.job.dto.request.JobSearchRequest;
import org.springframework.data.domain.Page;

public interface JobSearchRepositoryCustom {
    Page<JobDocument> search(JobSearchRequest request);
}