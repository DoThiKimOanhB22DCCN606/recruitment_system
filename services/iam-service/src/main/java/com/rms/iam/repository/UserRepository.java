package com.rms.iam.repository;

import com.rms.iam.entity.User;
import com.rms.iam.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByIdAndTenantId(UUID id, UUID tenantId);
    Page<User> findAllByTenantIdAndRole(UUID tenantId, com.rms.iam.entity.Role role, Pageable pageable);
    java.util.List<User> findByTenantIdAndStatus(UUID tenantId, UserStatus status);
    
    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE u.status = :status AND u.updatedAt <= :threshold")
    java.util.List<User> findDeactivatedUsersOlderThan(
            @org.springframework.data.repository.query.Param("status") UserStatus status,
            @org.springframework.data.repository.query.Param("threshold") java.time.Instant threshold);
}
