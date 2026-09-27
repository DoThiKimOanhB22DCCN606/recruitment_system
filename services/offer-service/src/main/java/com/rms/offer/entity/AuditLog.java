package com.rms.offer.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "offer_id", nullable = false)
    private UUID offerId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "old_status", length = 50)
    private String oldStatus;

    @Column(name = "new_status", nullable = false, length = 50)
    private String newStatus;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public AuditLog() {}

    public AuditLog(UUID id, UUID offerId, UUID tenantId, UUID actorId, String action, String oldStatus, String newStatus, String notes, Instant createdAt) {
        this.id = id;
        this.offerId = offerId;
        this.tenantId = tenantId;
        this.actorId = actorId;
        this.action = action;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getOfferId() { return offerId; }
    public void setOfferId(UUID offerId) { this.offerId = offerId; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public UUID getActorId() { return actorId; }
    public void setActorId(UUID actorId) { this.actorId = actorId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getOldStatus() { return oldStatus; }
    public void setOldStatus(String oldStatus) { this.oldStatus = oldStatus; }

    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID offerId;
        private UUID tenantId;
        private UUID actorId;
        private String action;
        private String oldStatus;
        private String newStatus;
        private String notes;
        private Instant createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder offerId(UUID offerId) { this.offerId = offerId; return this; }
        public Builder tenantId(UUID tenantId) { this.tenantId = tenantId; return this; }
        public Builder actorId(UUID actorId) { this.actorId = actorId; return this; }
        public Builder action(String action) { this.action = action; return this; }
        public Builder oldStatus(String oldStatus) { this.oldStatus = oldStatus; return this; }
        public Builder newStatus(String newStatus) { this.newStatus = newStatus; return this; }
        public Builder notes(String notes) { this.notes = notes; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public AuditLog build() {
            return new AuditLog(id, offerId, tenantId, actorId, action, oldStatus, newStatus, notes, createdAt);
        }
    }
}
