package com.rms.company.repository;

import com.rms.company.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByOwnerUserUuid(java.util.UUID ownerUserUuid);
    Optional<Company> findByOwnerUserId(Long ownerUserId);
    Optional<Company> findByTenantId(java.util.UUID tenantId);
}
