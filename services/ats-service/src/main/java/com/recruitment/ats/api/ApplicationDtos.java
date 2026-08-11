package com.recruitment.ats.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.recruitment.ats.domain.Application;
import com.recruitment.ats.domain.ApplicationStatus;
import com.recruitment.ats.domain.PipelineStage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ApplicationDtos {

    private ApplicationDtos() {
    }

    public record SubmitApplicationRequest(
            @NotNull UUID jobId,
            @NotNull UUID cvId,
            @Size(max = 2000) String coverLetter) {
    }

    public record MoveStageRequest(
            @NotNull UUID newStageId,
            @Size(max = 500) String reason) {
    }

    public record ApplicationResponse(
            UUID id,
            UUID tenantId,
            UUID jobId,
            UUID candidateId,
            UUID cvId,
            String coverLetter,
            ApplicationStatus status,
            UUID currentStageId,
            String currentStageName,
            String rejectionReason,
            Instant appliedAt,
            Instant updatedAt) {

        public static ApplicationResponse from(Application application) {
            return new ApplicationResponse(
                    application.getId(),
                    application.getTenantId(),
                    application.getJobId(),
                    application.getCandidateId(),
                    application.getCvId(),
                    application.getCoverLetter(),
                    application.getStatus(),
                    application.getCurrentStage().getId(),
                    application.getCurrentStage().getName(),
                    application.getRejectionReason(),
                    application.getAppliedAt(),
                    application.getUpdatedAt());
        }
    }

    public record StageColumn(
            UUID id,
            String name,
            int sequenceOrder,
            boolean terminal,
            long total,
            boolean hasMore,
            List<ApplicationResponse> applications) {

        public static StageColumn from(
                PipelineStage stage, List<Application> applications, long total, boolean hasMore) {
            return new StageColumn(stage.getId(), stage.getName(), stage.getSequenceOrder(),
                    stage.isTerminal(), total, hasMore,
                    applications.stream().map(ApplicationResponse::from).toList());
        }
    }

    public record PageResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages) {
    }

    public record HistoryResponse(
            UUID id,
            UUID fromStageId,
            String fromStageName,
            UUID toStageId,
            String toStageName,
            UUID actorId,
            String reason,
            Instant createdAt) {
    }

    public record WithdrawRequest(@Size(max = 500) String reason) {
    }
}
