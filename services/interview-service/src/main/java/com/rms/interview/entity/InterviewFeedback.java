package com.rms.interview.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "interview_feedbacks", uniqueConstraints = @UniqueConstraint(
        name = "uq_interview_feedback_reviewer", columnNames = {"interview_id", "reviewer_id"}))
public class InterviewFeedback {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "interview_id", nullable = false)
    private UUID interviewId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "reviewer_id", nullable = false)
    private UUID reviewerId;

    @Column(nullable = false)
    private Integer score;

    @Column(nullable = false, length = 2000)
    private String feedback;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InterviewRecommendation recommendation;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
