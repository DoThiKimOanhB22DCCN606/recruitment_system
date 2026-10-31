package com.rms.iam.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invitations")
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "invited_email", nullable = false)
    private String invitedEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by", nullable = false)
    private User invitedBy;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean accepted = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    public Invitation() {}

    public Invitation(Tenant tenant, String invitedEmail, Role role, User invitedBy, String tokenHash, Instant expiresAt) {
        this.tenant = tenant;
        this.invitedEmail = invitedEmail;
        this.role = role;
        this.invitedBy = invitedBy;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public String getInvitedEmail() { return invitedEmail; }
    public void setInvitedEmail(String invitedEmail) { this.invitedEmail = invitedEmail; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public User getInvitedBy() { return invitedBy; }
    public void setInvitedBy(User invitedBy) { this.invitedBy = invitedBy; }

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public boolean isAccepted() { return accepted; }
    public void setAccepted(boolean accepted) { this.accepted = accepted; }

    public Instant getCreatedAt() { return createdAt; }
}
