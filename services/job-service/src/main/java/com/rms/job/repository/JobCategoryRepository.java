package com.rms.job.repository;

import com.rms.job.entity.JobCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobCategoryRepository extends JpaRepository<JobCategory, Long> {
}
