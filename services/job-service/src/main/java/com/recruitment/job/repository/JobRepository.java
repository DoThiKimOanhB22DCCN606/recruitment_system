package com.recruitment.job.repository;

import com.recruitment.job.entity.Job;
import com.recruitment.job.entity.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findByCompanyId(Long companyId);

    /**
     * Full-text search using PostgreSQL tsvector/tsquery (GIN indexed) combined
     * with optional filters. `:keyword` is passed through plainto_tsquery so users
     * can type natural phrases. Empty/blank filter params are treated as "no filter"
     * via the (:param IS NULL OR ...) pattern.
     */
    @Query(value = """
        SELECT j.* FROM jobs j
        WHERE j.status = 'PUBLISHED'
          AND (:keyword IS NULL OR j.search_vector @@ plainto_tsquery('simple', :keyword))
          AND (:location IS NULL OR j.location ILIKE CONCAT('%', :location, '%'))
          AND (:employmentType IS NULL OR j.employment_type = :employmentType)
          AND (:experienceLevel IS NULL OR j.experience_level = :experienceLevel)
          AND (:remote IS NULL OR j.is_remote = :remote)
          AND (:categoryId IS NULL OR j.category_id = :categoryId)
          AND (:salaryMin IS NULL OR j.salary_max IS NULL OR j.salary_max >= :salaryMin)
          AND (:salaryMax IS NULL OR j.salary_min IS NULL OR j.salary_min <= :salaryMax)
        ORDER BY
          CASE WHEN :keyword IS NULL THEN 0
               ELSE ts_rank(j.search_vector, plainto_tsquery('simple', :keyword)) END DESC,
          j.published_at DESC
        """,
        countQuery = """
        SELECT count(*) FROM jobs j
        WHERE j.status = 'PUBLISHED'
          AND (:keyword IS NULL OR j.search_vector @@ plainto_tsquery('simple', :keyword))
          AND (:location IS NULL OR j.location ILIKE CONCAT('%', :location, '%'))
          AND (:employmentType IS NULL OR j.employment_type = :employmentType)
          AND (:experienceLevel IS NULL OR j.experience_level = :experienceLevel)
          AND (:remote IS NULL OR j.is_remote = :remote)
          AND (:categoryId IS NULL OR j.category_id = :categoryId)
          AND (:salaryMin IS NULL OR j.salary_max IS NULL OR j.salary_max >= :salaryMin)
          AND (:salaryMax IS NULL OR j.salary_min IS NULL OR j.salary_min <= :salaryMax)
        """,
        nativeQuery = true)
    Page<Job> search(
            @Param("keyword") String keyword,
            @Param("location") String location,
            @Param("employmentType") String employmentType,
            @Param("experienceLevel") String experienceLevel,
            @Param("remote") Boolean remote,
            @Param("categoryId") Long categoryId,
            @Param("salaryMin") java.math.BigDecimal salaryMin,
            @Param("salaryMax") java.math.BigDecimal salaryMax,
            Pageable pageable
    );

    @Query("SELECT j FROM Job j WHERE j.status = :status AND j.expiresAt IS NOT NULL AND j.expiresAt <= :now")
    List<Job> findExpiredJobs(@Param("status") JobStatus status, @Param("now") LocalDateTime now);

    Page<Job> findByStatus(JobStatus status, Pageable pageable);
}
