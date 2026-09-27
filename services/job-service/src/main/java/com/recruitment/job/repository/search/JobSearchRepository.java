package com.recruitment.job.repository.search;

import com.recruitment.job.document.JobDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface JobSearchRepository extends ElasticsearchRepository<JobDocument, String>, JobSearchRepositoryCustom {
    List<JobDocument> findByStatus(String status);
}
