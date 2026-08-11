package com.recruitment.interview.service;

import static com.recruitment.interview.api.ApiExceptionHandler.ApiException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.recruitment.interview.api.InterviewDtos.CreateInterviewRequest;
import com.recruitment.interview.api.InterviewDtos.InterviewResponse;
import com.recruitment.interview.api.InterviewDtos.UpdateInterviewRequest;
import com.recruitment.interview.config.RequestContext;
import com.recruitment.interview.domain.Interview;
import com.recruitment.interview.domain.InterviewStatus;
import com.recruitment.interview.integration.InternalServiceClient;
import com.recruitment.interview.messaging.NotificationEvent;
import com.recruitment.interview.messaging.NotificationPublisher;
import com.recruitment.interview.repository.InterviewRepository;

@Service
public class InterviewService {

    private final InterviewRepository repository;
    private final InternalServiceClient internalClient;
    private final NotificationPublisher notifications;

    public InterviewService(
            InterviewRepository repository,
            InternalServiceClient internalClient,
            NotificationPublisher notifications) {
        this.repository = repository;
        this.internalClient = internalClient;
        this.notifications = notifications;
    }

    @Transactional
    public InterviewResponse create(CreateInterviewRequest request) {
        UUID tenantId = requireEmployerTenant();
        var application = internalClient.getApplication(request.applicationId());
        if (application == null || !tenantId.equals(application.tenantId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ERR_APPLICATION_NOT_FOUND",
                    "Application was not found in this tenant");
        }
        if (!"ACTIVE".equals(application.status())) {
            throw new ApiException(HttpStatus.CONFLICT, "ERR_APPLICATION_NOT_ACTIVE",
                    "Interview can only be created for an active application");
        }
        validateFuture(request.scheduledDate(), request.startTime());
        validateInterviewers(request.interviewerIds(), tenantId);

        Interview interview = new Interview();
        interview.setApplicationId(request.applicationId());
        interview.setTenantId(tenantId);
        interview.setScheduledDate(request.scheduledDate());
        interview.setStartTime(request.startTime());
        interview.setDurationMinutes(request.durationMinutes());
        interview.setInterviewType(request.interviewType());
        interview.setLocationOrUrl(request.locationOrUrl());
        interview.setNotes(request.notes());
        interview.setCreatedBy(RequestContext.userId());
        interview.setParticipantIds(request.interviewerIds());
        interview = repository.save(interview);

        publishScheduleNotifications(interview, application.candidateId(), "INTERVIEW_SCHEDULED");
        return InterviewResponse.from(interview);
    }

    @Transactional(readOnly = true)
    public InterviewResponse get(UUID id) {
        return InterviewResponse.from(requireInterview(id));
    }

    @Transactional(readOnly = true)
    public List<InterviewResponse> listByApplication(UUID applicationId) {
        UUID tenantId = requireEmployerTenant();
        return repository.findByApplicationIdAndTenantIdOrderByScheduledDateAscStartTimeAsc(
                        applicationId, tenantId).stream()
                .map(InterviewResponse::from)
                .toList();
    }

    @Transactional
    public InterviewResponse update(UUID id, UpdateInterviewRequest request) {
        Interview interview = requireInterview(id);
        assertEditable(interview);

        LocalDate date = request.scheduledDate() == null
                ? interview.getScheduledDate() : request.scheduledDate();
        var time = request.startTime() == null ? interview.getStartTime() : request.startTime();
        validateFuture(date, time);

        if (request.interviewerIds() != null) {
            if (request.interviewerIds().isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "ERR_INTERVIEWER_REQUIRED",
                        "At least one interviewer is required");
            }
            validateInterviewers(request.interviewerIds(), interview.getTenantId());
            interview.setParticipantIds(request.interviewerIds());
        }
        interview.setScheduledDate(date);
        interview.setStartTime(time);
        if (request.durationMinutes() != null) {
            interview.setDurationMinutes(request.durationMinutes());
        }
        if (request.interviewType() != null) {
            interview.setInterviewType(request.interviewType());
        }
        if (request.locationOrUrl() != null) {
            interview.setLocationOrUrl(request.locationOrUrl());
        }
        if (request.notes() != null) {
            interview.setNotes(request.notes());
        }
        interview.setUpdatedAt(Instant.now());
        interview = repository.save(interview);

        var application = internalClient.getApplication(interview.getApplicationId());
        publishScheduleNotifications(interview, application.candidateId(), "INTERVIEW_UPDATED");
        return InterviewResponse.from(interview);
    }

    @Transactional
    public InterviewResponse cancel(UUID id) {
        Interview interview = requireInterview(id);
        if (interview.getStatus() == InterviewStatus.CANCELLED) {
            return InterviewResponse.from(interview);
        }
        if (interview.getStatus() == InterviewStatus.COMPLETED) {
            throw new ApiException(HttpStatus.CONFLICT, "ERR_INTERVIEW_COMPLETED",
                    "A completed interview cannot be cancelled");
        }

        interview.setStatus(InterviewStatus.CANCELLED);
        interview.setUpdatedAt(Instant.now());
        interview = repository.save(interview);
        var application = internalClient.getApplication(interview.getApplicationId());
        publishScheduleNotifications(interview, application.candidateId(), "INTERVIEW_CANCELLED");
        return InterviewResponse.from(interview);
    }

    private Interview requireInterview(UUID id) {
        return repository.findByIdAndTenantId(id, requireEmployerTenant())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ERR_INTERVIEW_NOT_FOUND",
                        "Interview was not found"));
    }

    private UUID requireEmployerTenant() {
        String role = RequestContext.role();
        if (!"RECRUITER".equals(role) && !"HR_ADMIN".equals(role)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ERR_INSUFFICIENT_PERMISSION",
                    "Only Recruiter or HR Admin may manage interviews");
        }
        UUID tenantId = RequestContext.tenantId();
        if (tenantId == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ERR_TENANT_REQUIRED",
                    "Interview operations require a tenant");
        }
        return tenantId;
    }

    private void assertEditable(Interview interview) {
        if (interview.getStatus() != InterviewStatus.SCHEDULED) {
            throw new ApiException(HttpStatus.CONFLICT, "ERR_INTERVIEW_NOT_EDITABLE",
                    "Only scheduled interviews can be edited");
        }
        if (LocalDateTime.of(interview.getScheduledDate(), interview.getStartTime())
                .isBefore(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.CONFLICT, "ERR_INTERVIEW_IN_PAST",
                    "Past interviews cannot be edited");
        }
    }

    private void validateFuture(LocalDate date, java.time.LocalTime time) {
        if (!LocalDateTime.of(date, time).isAfter(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ERR_INTERVIEW_TIME_INVALID",
                    "Interview time must be in the future");
        }
    }

    private void validateInterviewers(Set<UUID> interviewerIds, UUID tenantId) {
        for (UUID interviewerId : interviewerIds) {
            var user = internalClient.getUser(interviewerId, tenantId);
            boolean validRole = user != null
                    && ("RECRUITER".equals(user.role()) || "HR_ADMIN".equals(user.role()));
            if (!validRole || !tenantId.equals(user.tenantId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "ERR_INTERVIEWER_INVALID",
                        "All interviewers must be Recruiter or HR Admin in the same tenant");
            }
        }
    }

    private void publishScheduleNotifications(Interview interview, UUID candidateId, String actionType) {
        String action = switch (actionType) {
            case "INTERVIEW_CANCELLED" -> "cancelled";
            case "INTERVIEW_UPDATED" -> "updated";
            default -> "scheduled";
        };
        notifications.publish(new NotificationEvent(interview.getTenantId(), candidateId,
                actionType, "INTERVIEW", interview.getId(),
                "Interview " + action, "Your interview has been " + action));
        for (UUID interviewerId : interview.getParticipantIds()) {
            notifications.publish(new NotificationEvent(interview.getTenantId(), interviewerId,
                    actionType, "INTERVIEW", interview.getId(),
                    "Interview " + action, "An interview has been " + action));
        }
    }
}
