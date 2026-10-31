package com.rms.job.repository.search;

import com.rms.job.document.JobDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface JobSearchRepository extends ElasticsearchRepository<JobDocument, String>, JobSearchRepositoryCustom {
    List<JobDocument> findByStatus(String status);
}
