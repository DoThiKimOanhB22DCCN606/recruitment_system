package com.rms.iam.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Column(name = "failed_login_attempts")
    private int failedLoginAttempts = 0;

    @Column(name = "lockout_until")
    private Instant lockoutUntil;

    @Column(name = "privacy_accepted_at", nullable = false)
    private Instant privacyAcceptedAt;

    @Column(name = "pending_company_name")
    private String pendingCompanyName;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    public User() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }

    public Instant getLockoutUntil() { return lockoutUntil; }
    public void setLockoutUntil(Instant lockoutUntil) { this.lockoutUntil = lockoutUntil; }

    public Instant getPrivacyAcceptedAt() { return privacyAcceptedAt; }
    public void setPrivacyAcceptedAt(Instant privacyAcceptedAt) { this.privacyAcceptedAt = privacyAcceptedAt; }

    public String getPendingCompanyName() { return pendingCompanyName; }
    public void setPendingCompanyName(String pendingCompanyName) { this.pendingCompanyName = pendingCompanyName; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
