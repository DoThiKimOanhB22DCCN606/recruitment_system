package com.recruitment.ats.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
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
@Table(name = "pipeline_stages", uniqueConstraints = {
        @UniqueConstraint(name = "uk_stage_job_order", columnNames = {"job_id", "sequence_order"}),
        @UniqueConstraint(name = "uk_stage_job_name", columnNames = {"job_id", "name"})
})
public class PipelineStage {
    @Id
    @GeneratedValue
    private UUID id;
    private UUID tenantId;
    private UUID jobId;
    private String name;
    private int sequenceOrder;
    private boolean terminal;
    @Enumerated(EnumType.STRING)
    private ApplicationStatus terminalStatus;
    private Instant createdAt = Instant.now();

    public PipelineStage(UUID tenantId, UUID jobId, String name, int sequenceOrder,
                         boolean terminal, ApplicationStatus terminalStatus) {
        this.tenantId = tenantId;
        this.jobId = jobId;
        this.name = name;
        this.sequenceOrder = sequenceOrder;
        this.terminal = terminal;
        this.terminalStatus = terminalStatus;
    }
}
