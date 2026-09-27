package com.recruitment.job.repository.search;

import com.recruitment.job.document.JobDocument;
import com.recruitment.job.dto.request.JobSearchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class JobSearchRepositoryImpl implements JobSearchRepositoryCustom {

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public Page<JobDocument> search(JobSearchRequest request) {
        int page = Math.max(0, request.getPage());
        int size = request.getSize() > 0 ? Math.min(request.getSize(), 50) : 20;

        boolean hasKeyword = request.getKeyword() != null && !request.getKeyword().trim().isEmpty();
        org.springframework.data.domain.Sort sort;
        if ("salary".equalsIgnoreCase(request.getSortBy()) || "salaryMax".equalsIgnoreCase(request.getSortBy())) {
            org.springframework.data.domain.Sort.Direction direction = "asc".equalsIgnoreCase(request.getSortOrder()) ? org.springframework.data.domain.Sort.Direction.ASC : org.springframework.data.domain.Sort.Direction.DESC;
            sort = org.springframework.data.domain.Sort.by(direction, "salaryMax");
        } else if ("date".equalsIgnoreCase(request.getSortBy()) || "publishedAt".equalsIgnoreCase(request.getSortBy()) || "createdAt".equalsIgnoreCase(request.getSortBy())) {
            org.springframework.data.domain.Sort.Direction direction = "asc".equalsIgnoreCase(request.getSortOrder()) ? org.springframework.data.domain.Sort.Direction.ASC : org.springframework.data.domain.Sort.Direction.DESC;
            sort = org.springframework.data.domain.Sort.by(direction, "publishedAt");
        } else if ("relevance".equalsIgnoreCase(request.getSortBy()) || "_score".equalsIgnoreCase(request.getSortBy()) || (hasKeyword && (request.getSortBy() == null || request.getSortBy().trim().isEmpty()))) {
            sort = org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Order.desc("_score"),
                    org.springframework.data.domain.Sort.Order.desc("publishedAt")
            );
        } else {
            sort = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "publishedAt");
        }
        Pageable pageable = PageRequest.of(page, size, sort);

        Criteria criteria = new Criteria("status").is("PUBLISHED");

        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            String kw = request.getKeyword().trim();
            criteria = criteria.subCriteria(
                new Criteria("title").contains(kw)
                    .or(new Criteria("description").contains(kw))
                    .or(new Criteria("requirements").contains(kw))
            );
        }

        if (request.getSkills() != null && !request.getSkills().isEmpty()) {
            criteria = criteria.and(new Criteria("skills").in(request.getSkills()));
        } else if (request.getSkill() != null && !request.getSkill().trim().isEmpty()) {
            criteria = criteria.and(new Criteria("skills").is(request.getSkill().trim()));
        }

        if (request.getLocation() != null && !request.getLocation().trim().isEmpty()) {
            criteria = criteria.and(new Criteria("location").is(request.getLocation().trim()));
        }

        if (request.getEmploymentType() != null && !request.getEmploymentType().trim().isEmpty()) {
            criteria = criteria.and(new Criteria("employmentType").is(request.getEmploymentType().trim()));
        }

        if (request.getExperienceLevel() != null && !request.getExperienceLevel().trim().isEmpty()) {
            criteria = criteria.and(new Criteria("experienceLevel").is(request.getExperienceLevel().trim()));
        }

        if (request.getWorkType() != null && !request.getWorkType().trim().isEmpty()) {
            criteria = criteria.and(new Criteria("workType").is(request.getWorkType().trim()));
        }

        if (request.getCategoryId() != null) {
            criteria = criteria.and(new Criteria("categoryId").is(request.getCategoryId()));
        }

        if (request.getSalaryMin() != null) {
            criteria = criteria.and(new Criteria("salaryMax").greaterThanEqual(request.getSalaryMin().doubleValue()));
        }

        if (request.getSalaryMax() != null) {
            criteria = criteria.and(new Criteria("salaryMin").lessThanEqual(request.getSalaryMax().doubleValue()));
        }

        CriteriaQuery query = new CriteriaQuery(criteria, pageable);
        SearchHits<JobDocument> searchHits = elasticsearchOperations.search(query, JobDocument.class);

        List<JobDocument> list = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();

        return new PageImpl<>(list, pageable, searchHits.getTotalHits());
    }
}