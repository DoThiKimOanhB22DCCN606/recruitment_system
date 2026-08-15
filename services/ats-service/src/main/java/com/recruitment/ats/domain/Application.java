package com.recruitment.ats.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "applications", uniqueConstraints =
        @UniqueConstraint(name = "uk_application_job_candidate", columnNames = {"job_id", "candidate_id"}))
public class Application {
    @Id
    @GeneratedValue
    private UUID id;
    private UUID tenantId;
    private UUID jobId;
    private UUID candidateId;
    private UUID cvId;
    @Column(length = 2000)
    private String coverLetter;
    @Enumerated(EnumType.STRING)
    private ApplicationStatus status = ApplicationStatus.ACTIVE;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "current_stage_id")
    private PipelineStage currentStage;
    @Column(length = 500)
    private String rejectionReason;
    private Instant appliedAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public boolean isTerminal() {
        return status != ApplicationStatus.ACTIVE;
    }
}
