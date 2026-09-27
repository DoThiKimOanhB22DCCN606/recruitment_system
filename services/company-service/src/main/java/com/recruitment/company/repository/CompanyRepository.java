package com.recruitment.company.repository;

import com.recruitment.company.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByOwnerUserUuid(java.util.UUID ownerUserUuid);
    Optional<Company> findByOwnerUserId(Long ownerUserId);
}
