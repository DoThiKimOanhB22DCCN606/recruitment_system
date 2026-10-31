package com.rms.ats.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rms.ats.entity.IdempotencyKey;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {
}
