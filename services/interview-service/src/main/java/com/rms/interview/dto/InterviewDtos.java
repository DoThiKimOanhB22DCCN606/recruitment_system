package com.rms.interview.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.rms.interview.entity.Interview;
import com.rms.interview.entity.InterviewStatus;
import com.rms.interview.entity.InterviewType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class InterviewDtos {

    private InterviewDtos() {
    }

    public record CreateInterviewRequest(
            @NotNull UUID applicationId,
            LocalDate scheduledDate,
            LocalTime startTime,
            @Min(15) @Max(480) int durationMinutes,
            @NotNull InterviewType interviewType,
            @Size(max = 500) String locationOrUrl,
            @Size(max = 2000) String notes,
            @Size(max = 10) Set<UUID> interviewerIds,
            Instant scheduledAt,
            @Size(max = 500) String meetingUrl,
            @Size(max = 500) String location,
            @Size(max = 10) List<@Size(max = 320) String> interviewerEmails) {

        public CreateInterviewRequest(
                UUID applicationId, LocalDate scheduledDate, LocalTime startTime, int durationMinutes,
                InterviewType interviewType, String locationOrUrl, String notes, Set<UUID> interviewerIds) {
            this(applicationId, scheduledDate, startTime, durationMinutes, interviewType,
                    locationOrUrl, notes, interviewerIds, null, null, null, null);
        }
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
