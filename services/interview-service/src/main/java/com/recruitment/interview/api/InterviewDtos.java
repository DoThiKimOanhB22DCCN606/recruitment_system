package com.recruitment.interview.api;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import com.recruitment.interview.domain.Interview;
import com.recruitment.interview.domain.InterviewStatus;
import com.recruitment.interview.domain.InterviewType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class InterviewDtos {

    private InterviewDtos() {
    }

    public record CreateInterviewRequest(
            @NotNull UUID applicationId,
            @NotNull LocalDate scheduledDate,
            @NotNull LocalTime startTime,
            @Min(15) @Max(480) int durationMinutes,
            @NotNull InterviewType interviewType,
            @Size(max = 500) String locationOrUrl,
            @Size(max = 2000) String notes,
            @NotNull @Size(min = 1, max = 10) Set<UUID> interviewerIds) {
    }

    public record UpdateInterviewRequest(
            LocalDate scheduledDate,
            LocalTime startTime,
            @Min(15) @Max(480) Integer durationMinutes,
            InterviewType interviewType,
            @Size(max = 500) String locationOrUrl,
            @Size(max = 2000) String notes,
            @Size(min = 1, max = 10) Set<UUID> interviewerIds) {
    }

    public record InterviewResponse(
            UUID id,
            UUID applicationId,
            UUID tenantId,
            LocalDate scheduledDate,
            LocalTime startTime,
            int durationMinutes,
            InterviewType interviewType,
            String locationOrUrl,
            String notes,
            InterviewStatus status,
            Set<UUID> interviewerIds,
            UUID createdBy,
            Instant createdAt,
            Instant updatedAt) {

        public static InterviewResponse from(Interview interview) {
            return new InterviewResponse(
                    interview.getId(), interview.getApplicationId(), interview.getTenantId(),
                    interview.getScheduledDate(), interview.getStartTime(), interview.getDurationMinutes(),
                    interview.getInterviewType(), interview.getLocationOrUrl(), interview.getNotes(),
                    interview.getStatus(), interview.getParticipantIds(), interview.getCreatedBy(),
                    interview.getCreatedAt(), interview.getUpdatedAt());
        }
    }
}
