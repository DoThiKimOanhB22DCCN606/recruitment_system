package com.rms.interview.service;

import static com.rms.interview.exception.ApiExceptionHandler.ApiException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import com.rms.interview.dto.InterviewDtos.CreateInterviewRequest;
import com.rms.interview.dto.InterviewDtos.InterviewResponse;
import com.rms.interview.dto.InterviewDtos.TimeSlotRequest;
import com.rms.interview.dto.InterviewDtos.UpdateInterviewRequest;
import com.rms.interview.config.RequestContext;
import com.rms.interview.entity.Interview;
import com.rms.interview.entity.InterviewTimeSlot;
import com.rms.interview.entity.InterviewStatus;
import com.rms.interview.client.InternalServiceClient;
import com.rms.interview.messaging.NotificationEvent;
import com.rms.interview.messaging.NotificationPublisher;
import com.rms.interview.repository.InterviewRepository;
import com.rms.interview.repository.InterviewFeedbackRepository;
import com.rms.interview.dto.InterviewFeedbackDtos.FeedbackRequest;
import com.rms.interview.dto.InterviewFeedbackDtos.FeedbackResponse;
import com.rms.interview.entity.InterviewFeedback;

@Service
public class InterviewService {

    private final InterviewRepository repository;
    private final InterviewFeedbackRepository feedbackRepository;
    private final InternalServiceClient internalClient;
    private final NotificationPublisher notifications;
    private final JdbcTemplate jdbcTemplate;

    public InterviewService(
            InterviewRepository repository,
            InterviewFeedbackRepository feedbackRepository,
            InternalServiceClient internalClient,
            NotificationPublisher notifications,
            JdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.feedbackRepository = feedbackRepository;
        this.internalClient = internalClient;
        this.notifications = notifications;
        this.jdbcTemplate = jdbcTemplate;
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
        boolean candidateChoosesSlot = request.timeSlots() != null && !request.timeSlots().isEmpty();
        LocalDate scheduledDate = request.scheduledDate();
        java.time.LocalTime startTime = request.startTime();
        if (!candidateChoosesSlot && (scheduledDate == null || startTime == null) && request.scheduledAt() != null) {
            LocalDateTime localDateTime = LocalDateTime.ofInstant(
                    request.scheduledAt(), ZoneId.of("Asia/Ho_Chi_Minh"));
            scheduledDate = localDateTime.toLocalDate();
            startTime = localDateTime.toLocalTime();
        }
        if (candidateChoosesSlot) {
            validateTimeSlots(request.timeSlots());
        } else {
            if (scheduledDate == null || startTime == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "ERR_INTERVIEW_TIME_REQUIRED",
                        "Provide interview time options for the candidate to choose from");
            }
            validateFuture(scheduledDate, startTime);
        }

        Set<UUID> interviewerIds = new LinkedHashSet<>();
        if (request.interviewerIds() != null) interviewerIds.addAll(request.interviewerIds());
        if (request.interviewerEmails() != null) {
            for (String email : request.interviewerEmails()) {
                if (email == null || email.isBlank()) continue;
                try {
                    var user = internalClient.getUserByEmail(email.trim(), tenantId);
                    if (user == null || !tenantId.equals(user.tenantId())
                            || (!"RECRUITER".equals(user.role()) && !"HR_ADMIN".equals(user.role()))) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "ERR_INTERVIEWER_INVALID",
                                "All interviewers must be Recruiter or HR Admin in the same tenant");
                    }
                    interviewerIds.add(user.id());
                } catch (ApiException e) {
                    throw e;
                } catch (RuntimeException e) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "ERR_INTERVIEWER_INVALID",
                            "Interviewer email was not found in this tenant: " + email);
                }
            }
        }
        if (interviewerIds.isEmpty()) interviewerIds.add(RequestContext.userId());
        validateInterviewers(interviewerIds, tenantId);

        String locationOrUrl = request.meetingUrl() != null && !request.meetingUrl().isBlank()
                ? request.meetingUrl().trim()
                : request.location() != null && !request.location().isBlank()
                        ? request.location().trim() : request.locationOrUrl();

        Interview interview = new Interview();
        interview.setApplicationId(request.applicationId());
        interview.setTenantId(tenantId);
        interview.setCandidateId(application.candidateId());
        interview.setScheduledDate(candidateChoosesSlot ? null : scheduledDate);
        interview.setStartTime(candidateChoosesSlot ? null : startTime);
        interview.setDurationMinutes(request.durationMinutes());
        interview.setInterviewType(request.interviewType());
        interview.setLocationOrUrl(locationOrUrl);
        interview.setNotes(request.notes());
        interview.setCreatedBy(RequestContext.userId());
        interview.setParticipantIds(interviewerIds);
        if (candidateChoosesSlot) {
            interview.setTimeSlots(request.timeSlots().stream()
                    .map(slot -> new InterviewTimeSlot(slot.date(), slot.startTime(), slot.endTime()))
                    .toList());
            interview.setStatus(InterviewStatus.AWAITING_CANDIDATE_SELECTION);
        } else {
            LocalDateTime proposedStart = LocalDateTime.of(scheduledDate, startTime);
            lockCandidate(application.candidateId());
            rejectIfOverlapping(interview, application.candidateId(), proposedStart,
                    proposedStart.plusMinutes(request.durationMinutes()), request.applicationId());
        }
        interview = repository.save(interview);

        if (candidateChoosesSlot) {
            publishSlotOptionsNotification(interview, application);
        } else {
            publishScheduleNotifications(interview, application, "INTERVIEW_SCHEDULED");
        }
        return InterviewResponse.from(interview);
    }

    @Transactional
    public InterviewResponse selectTimeSlot(UUID interviewId, int slotIndex) {
        String role = RequestContext.role().replaceFirst("(?i)^ROLE_", "");
        if (!"CANDIDATE".equalsIgnoreCase(role)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ERR_INSUFFICIENT_PERMISSION",
                    "Only the candidate may select an interview time");
        }
        Interview interview = repository.findById(interviewId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ERR_INTERVIEW_NOT_FOUND",
                        "Interview was not found"));
        UUID candidateId = RequestContext.userId();
        if (interview.getCandidateId() == null || !candidateId.equals(interview.getCandidateId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ERR_INTERVIEW_NOT_FOUND",
                    "Interview was not found");
        }
        if (interview.getStatus() != InterviewStatus.AWAITING_CANDIDATE_SELECTION) {
            throw new ApiException(HttpStatus.CONFLICT, "ERR_INTERVIEW_SLOT_CLOSED",
                    "This interview no longer accepts a time selection");
        }
        if (slotIndex < 0 || slotIndex >= interview.getTimeSlots().size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ERR_INTERVIEW_SLOT_INVALID",
                    "Choose one of the available interview time options");
        }

        InterviewTimeSlot selected = interview.getTimeSlots().get(slotIndex);
        LocalDateTime startsAt = LocalDateTime.of(selected.getDate(), selected.getStartTime());
        LocalDateTime endsAt = LocalDateTime.of(selected.getDate(), selected.getEndTime());
        lockCandidate(candidateId);
        rejectIfOverlapping(interview, candidateId, startsAt, endsAt, null);

        interview.setScheduledDate(selected.getDate());
        interview.setStartTime(selected.getStartTime());
        interview.setDurationMinutes(Math.toIntExact(Duration.between(startsAt, endsAt).toMinutes()));
        interview.setCandidateSelectedSlotIndex(slotIndex);
        interview.setStatus(InterviewStatus.SCHEDULED);
        interview.setUpdatedAt(Instant.now());
        interview = repository.save(interview);
        publishSlotSelectedNotification(interview, candidateId, startsAt, endsAt);
        return InterviewResponse.from(interview);
    }

    @Transactional(readOnly = true)
    public InterviewResponse get(UUID id) {
        return InterviewResponse.from(requireInterview(id));
    }

    @Transactional(readOnly = true)
    public List<InterviewResponse> list() {
        UUID tenantId = requireEmployerTenant();
        return repository.findByTenantIdOrderByScheduledDateAscStartTimeAsc(tenantId).stream()
                .map(InterviewResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InterviewResponse> listForCurrentCandidate() {
        String role = RequestContext.role().replaceFirst("(?i)^ROLE_", "");
        if (!"CANDIDATE".equalsIgnoreCase(role)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ERR_INSUFFICIENT_PERMISSION",
                    "Only candidates may view their own interviews");
        }
        List<UUID> applicationIds = internalClient.getCandidateApplicationIds(RequestContext.userId());
        if (applicationIds.isEmpty()) {
            return List.of();
        }
        return repository.findByApplicationIdInOrderByScheduledDateAscStartTimeAsc(applicationIds).stream()
                .map(InterviewResponse::from)
                .toList();
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
        int updatedDuration = request.durationMinutes() == null
                ? interview.getDurationMinutes() : request.durationMinutes();
        if (interview.getCandidateId() != null) {
            LocalDateTime startsAt = LocalDateTime.of(date, time);
            lockCandidate(interview.getCandidateId());
            rejectIfOverlapping(interview, interview.getCandidateId(), startsAt,
                    startsAt.plusMinutes(updatedDuration), null);
        }

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
        interview.setDurationMinutes(updatedDuration);
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
        publishScheduleNotifications(interview, application, "INTERVIEW_UPDATED");
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
        publishScheduleNotifications(interview, application, "INTERVIEW_CANCELLED");
        return InterviewResponse.from(interview);
    }

    @Transactional
    public FeedbackResponse submitFeedback(UUID interviewId, FeedbackRequest request) {
        Interview interview = requireInterview(interviewId);
        UUID reviewerId = RequestContext.userId();
        InterviewFeedback feedback = feedbackRepository
                .findByInterviewIdAndReviewerId(interviewId, reviewerId)
                .orElseGet(InterviewFeedback::new);
        feedback.setInterviewId(interviewId);
        feedback.setTenantId(interview.getTenantId());
        feedback.setReviewerId(reviewerId);
        feedback.setScore(request.score());
        feedback.setFeedback(request.feedback().trim());
        feedback.setRecommendation(request.recommendation());
        feedback.setUpdatedAt(Instant.now());
        return FeedbackResponse.from(feedbackRepository.save(feedback));
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> getFeedbacks(UUID interviewId) {
        Interview interview = requireInterview(interviewId);
        return feedbackRepository.findByInterviewIdAndTenantIdOrderByCreatedAtDesc(
                        interview.getId(), interview.getTenantId()).stream()
                .map(FeedbackResponse::from)
                .toList();
    }

    private Interview requireInterview(UUID id) {
        return repository.findByIdAndTenantId(id, requireEmployerTenant())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ERR_INTERVIEW_NOT_FOUND",
                        "Interview was not found"));
    }

    private UUID requireEmployerTenant() {
        String role = RequestContext.role().replaceFirst("(?i)^ROLE_", "");
        if (!"RECRUITER".equalsIgnoreCase(role) && !"HR_ADMIN".equalsIgnoreCase(role)) {
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

    private void validateTimeSlots(List<TimeSlotRequest> slots) {
        for (int i = 0; i < slots.size(); i++) {
            TimeSlotRequest slot = slots.get(i);
            if (slot.date() == null || slot.startTime() == null || slot.endTime() == null
                    || !slot.endTime().isAfter(slot.startTime())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "ERR_INTERVIEW_SLOT_INVALID",
                        "Each option must have a date and an end time later than its start time");
            }
            long minutes = Duration.between(slot.startTime(), slot.endTime()).toMinutes();
            if (minutes < 15 || minutes > 480) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "ERR_INTERVIEW_SLOT_DURATION",
                        "Each interview time option must be between 15 minutes and 8 hours");
            }
            validateFuture(slot.date(), slot.startTime());
            for (int j = 0; j < i; j++) {
                TimeSlotRequest earlier = slots.get(j);
                boolean overlaps = earlier.date().equals(slot.date())
                        && earlier.startTime().isBefore(slot.endTime())
                        && slot.startTime().isBefore(earlier.endTime());
                if (overlaps) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "ERR_INTERVIEW_SLOT_OVERLAP",
                            "The proposed time options must not overlap each other");
                }
            }
        }
    }

    private void lockCandidate(UUID candidateId) {
        jdbcTemplate.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) connection -> {
            try (var statement = connection.prepareStatement("SELECT pg_advisory_xact_lock(hashtext(?))")) {
                statement.setString(1, candidateId.toString());
                try (var result = statement.executeQuery()) {
                    if (result.next()) result.getObject(1);
                }
            }
            return null;
        });
    }

    private void rejectIfOverlapping(Interview proposed, UUID candidateId,
            LocalDateTime startsAt, LocalDateTime endsAt, UUID fallbackEntityId) {
        var nearbyDays = repository.findPotentialConflicts(
                candidateId, InterviewStatus.SCHEDULED.name(),
                startsAt.toLocalDate().minusDays(1), endsAt.toLocalDate());
        boolean overlaps = nearbyDays.stream()
                .filter(existing -> proposed.getId() == null || !existing.getId().equals(proposed.getId()))
                .filter(existing -> existing.getScheduledDate() != null && existing.getStartTime() != null)
                .anyMatch(existing -> {
                    LocalDateTime existingStart = LocalDateTime.of(
                            existing.getScheduledDate(), existing.getStartTime());
                    LocalDateTime existingEnd = existingStart.plusMinutes(existing.getDurationMinutes());
                    return existingStart.isBefore(endsAt) && startsAt.isBefore(existingEnd);
                });
        if (overlaps) {
            UUID entityId = proposed.getId() != null ? proposed.getId() : fallbackEntityId;
            notifications.publish(new NotificationEvent(proposed.getTenantId(), candidateId,
                    "INTERVIEW_SLOT_CONFLICT", "INTERVIEW", entityId,
                    "Khung giờ phỏng vấn bị trùng",
                    "Khung giờ " + startsAt.format(DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy"))
                            + " - " + endsAt.format(DateTimeFormatter.ofPattern("HH:mm"))
                            + " trùng với một lịch phỏng vấn khác. Vui lòng chọn khung giờ khác."));
            throw new ApiException(HttpStatus.CONFLICT, "ERR_INTERVIEW_TIME_CONFLICT",
                    "Khung giờ này trùng với một lịch phỏng vấn khác của bạn. Vui lòng chọn khung giờ khác.");
        }
    }

    private void publishSlotOptionsNotification(Interview interview,
            InternalServiceClient.ApplicationDetails application) {
        String options = interview.getTimeSlots().stream()
                .map(slot -> slot.getStartTime() + "-" + slot.getEndTime() + " ngày "
                        + slot.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .collect(java.util.stream.Collectors.joining(", "));
        notifications.publish(new NotificationEvent(interview.getTenantId(), application.candidateId(),
                "INTERVIEW_SLOTS_PROPOSED", "INTERVIEW", interview.getId(),
                "Vui lòng chọn khung giờ phỏng vấn",
                "Nhà tuyển dụng đã đề xuất các khung giờ: " + options
                        + ". Vui lòng vào mục Lịch phỏng vấn để chọn một khung phù hợp."));
    }

    private void publishSlotSelectedNotification(Interview interview, UUID candidateId,
            LocalDateTime startsAt, LocalDateTime endsAt) {
        String when = startsAt.format(DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy"))
                + " - " + endsAt.format(DateTimeFormatter.ofPattern("HH:mm"));
        notifications.publish(new NotificationEvent(interview.getTenantId(), candidateId,
                "INTERVIEW_SLOT_SELECTED", "INTERVIEW", interview.getId(),
                "Bạn đã xác nhận lịch phỏng vấn",
                "Bạn đã chọn khung giờ phỏng vấn " + when + "."));
        for (UUID interviewerId : interview.getParticipantIds()) {
            notifications.publish(new NotificationEvent(interview.getTenantId(), interviewerId,
                    "INTERVIEW_SLOT_SELECTED", "INTERVIEW", interview.getId(),
                    "Ứng viên đã chọn khung giờ phỏng vấn",
                    "Ứng viên đã xác nhận khung giờ phỏng vấn " + when + "."));
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

    private void publishScheduleNotifications(Interview interview,
                                              InternalServiceClient.ApplicationDetails application,
                                              String actionType) {
        String action = switch (actionType) {
            case "INTERVIEW_CANCELLED" -> "đã hủy";
            case "INTERVIEW_UPDATED" -> "đã cập nhật";
            default -> "đã lên lịch";
        };
        String title = switch (actionType) {
            case "INTERVIEW_CANCELLED" -> "Lịch phỏng vấn đã hủy";
            case "INTERVIEW_UPDATED" -> "Lịch phỏng vấn đã thay đổi";
            default -> "Có lịch phỏng vấn mới";
        };
        String actor = "phụ trách";
        try {
            var user = internalClient.getUser(RequestContext.userId(), interview.getTenantId());
            if (user != null) actor = displayName(user.fullName(), user.email(), actor);
        } catch (RuntimeException ignored) {
            // Notification delivery must continue when IAM is temporarily unavailable.
        }
        String candidate = displayName(application.candidateName(), application.candidateEmail(),
                "ứng viên");
        String when = LocalDateTime.of(interview.getScheduledDate(), interview.getStartTime())
                .format(DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy"));
        String location = interview.getLocationOrUrl() != null && !interview.getLocationOrUrl().isBlank()
                ? " Địa điểm/liên kết: " + interview.getLocationOrUrl() + "." : "";
        notifications.publish(new NotificationEvent(interview.getTenantId(), application.candidateId(),
                actionType, "INTERVIEW", interview.getId(), title,
                "Nhà tuyển dụng " + actor + " " + action + " lịch phỏng vấn của bạn lúc " + when + "." + location));
        for (UUID interviewerId : interview.getParticipantIds()) {
            notifications.publish(new NotificationEvent(interview.getTenantId(), interviewerId,
                    actionType, "INTERVIEW", interview.getId(), title,
                    "Nhà tuyển dụng " + actor + " " + action + " lịch phỏng vấn ứng viên "
                            + candidate + " lúc " + when + "." + location));
        }
    }

    private String displayName(String fullName, String email, String fallback) {
        if (fullName != null && !fullName.isBlank() && !"New Candidate".equalsIgnoreCase(fullName)) {
            return fullName.trim();
        }
        if (email != null && !email.isBlank() && !email.endsWith("@placeholder.local")) {
            return email;
        }
        return fallback;
    }
}
