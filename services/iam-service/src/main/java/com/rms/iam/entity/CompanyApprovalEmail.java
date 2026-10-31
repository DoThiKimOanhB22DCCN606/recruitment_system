package com.rms.iam.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "company_approval_emails")
public class CompanyApprovalEmail {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
    public Instant getCreatedAt() { return createdAt; }
}
