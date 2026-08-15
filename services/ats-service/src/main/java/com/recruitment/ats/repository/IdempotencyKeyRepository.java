package com.recruitment.ats.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.recruitment.ats.domain.IdempotencyKey;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {
}
