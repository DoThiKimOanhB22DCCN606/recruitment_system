package com.rms.iam.repository;

import com.rms.iam.entity.CompanyApprovalEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CompanyApprovalEmailRepository extends JpaRepository<CompanyApprovalEmail, UUID> {
    List<CompanyApprovalEmail> findTop20BySentAtIsNullOrderByCreatedAtAsc();
}
