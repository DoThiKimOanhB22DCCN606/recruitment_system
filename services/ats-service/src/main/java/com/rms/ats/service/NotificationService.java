package com.rms.ats.service;

import static com.rms.ats.exception.ApiExceptionHandler.ApiException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rms.ats.config.RequestContext;
import com.rms.ats.client.InternalServiceClient;
import com.rms.ats.entity.Application;
import com.rms.ats.entity.ApplicationStageHistory;
import com.rms.ats.entity.Notification;
import com.rms.ats.repository.ApplicationRepository;
import com.rms.ats.repository.ApplicationStageHistoryRepository;
import com.rms.ats.repository.NotificationRepository;
import com.rms.ats.repository.PipelineStageRepository;

@Service
public class NotificationService {

    private final NotificationRepository repository;
    private final ApplicationRepository applications;
    private final ApplicationStageHistoryRepository history;
    private final PipelineStageRepository stages;
    private final InternalServiceClient internalClient;

    public NotificationService(NotificationRepository repository, ApplicationRepository applications,
                               ApplicationStageHistoryRepository history, PipelineStageRepository stages,
                               InternalServiceClient internalClient) {
        this.repository = repository;
        this.applications = applications;
        this.history = history;
        this.stages = stages;
        this.internalClient = internalClient;
    }

    @Transactional(readOnly = true)
    public List<Notification> list() {
        return repository.findByRecipientUserIdAndCreatedAtAfterOrderByCreatedAtDesc(
                RequestContext.userId(), Instant.now().minus(90, ChronoUnit.DAYS)).stream()
                .map(this::localizeLegacy)
                .toList();
    }

    private Notification localizeLegacy(Notification original) {
        if (original.getTitle() == null || !original.getTitle().matches("[A-Za-z].*")) return original;
        Notification localized = new Notification();
        localized.setId(original.getId());
        localized.setTenantId(original.getTenantId());
        localized.setRecipientUserId(original.getRecipientUserId());
        localized.setActionType(original.getActionType());
        localized.setEntityType(original.getEntityType());
        localized.setEntityId(original.getEntityId());
        localized.setRead(original.isRead());
        localized.setCreatedAt(original.getCreatedAt());
        localized.setReadAt(original.getReadAt());
        localized.setTitle(original.getTitle());
        localized.setMessage(original.getMessage());

        String type = original.getActionType();
        if (type == null) return localized;
        switch (type) {
            case "APPLICATION_SUBMITTED" -> {
                localized.setTitle("Có hồ sơ ứng tuyển mới");
                localized.setMessage(applicationMessage(original, "đã ứng tuyển vào vị trí"));
            }
            case "APPLICATION_STAGE_CHANGED" -> {
                localized.setTitle("Cập nhật quy trình ứng tuyển");
                localized.setMessage(legacyStageMessage(original));
            }
            case "APPLICATION_WITHDRAWN" -> {
                localized.setTitle("Ứng viên đã rút hồ sơ");
                localized.setMessage(applicationMessage(original, "đã rút hồ sơ ứng tuyển khỏi vị trí"));
            }
            case "APPLICATION_HIRED" -> {
                localized.setTitle("Ứng viên đã nhận việc");
                localized.setMessage(applicationMessage(original, "đã nhận việc ở vị trí"));
            }
            case "OFFER_EXPIRED" -> {
                localized.setTitle("Lời mời nhận việc đã hết hạn");
                localized.setMessage(applicationMessage(original, "có lời mời nhận việc đã hết hạn cho vị trí"));
            }
            case "INTERVIEW_SCHEDULED" -> {
                localized.setTitle("Có lịch phỏng vấn mới");
                localized.setMessage("Lịch phỏng vấn đã được tạo. Xem mục Lịch phỏng vấn để biết thời gian và người tham gia.");
            }
            case "INTERVIEW_UPDATED" -> {
                localized.setTitle("Lịch phỏng vấn đã thay đổi");
                localized.setMessage("Lịch phỏng vấn đã được cập nhật. Xem mục Lịch phỏng vấn để biết thông tin mới.");
            }
            case "INTERVIEW_CANCELLED" -> {
                localized.setTitle("Lịch phỏng vấn đã hủy");
                localized.setMessage("Lịch phỏng vấn đã bị hủy. Xem mục Lịch phỏng vấn để biết chi tiết.");
            }
            case "OPEN_TO_WORK_EXPIRING_SOON" -> {
                localized.setTitle("Trạng thái tìm việc sắp hết hạn");
                localized.setMessage("Trạng thái tìm việc sẽ hết hạn sau 3 ngày. Hãy gia hạn trong hồ sơ cá nhân.");
            }
            case "OPEN_TO_WORK_EXPIRED" -> {
                localized.setTitle("Trạng thái tìm việc đã hết hạn");
                localized.setMessage("Trạng thái tìm việc đã hết hạn và được chuyển về chế độ không tìm việc.");
            }
            default -> {
                return original;
            }
        }
        return localized;
    }

    private String applicationMessage(Notification notification, String action) {
        var application = application(notification);
        if (application == null) return "Hồ sơ ứng tuyển đã được cập nhật. Vui lòng xem Quy trình ATS để biết chi tiết.";
        return "Ứng viên " + candidateName(application) + " " + action + " " + jobName(application) + ".";
    }

    private String legacyStageMessage(Notification notification) {
        var application = application(notification);
        if (application == null) return "Hồ sơ ứng tuyển của bạn đã được chuyển bước trong quy trình tuyển dụng.";
        String target = notification.getMessage() == null ? "" : notification.getMessage()
                .replaceFirst("^Your application moved to ", "");
        String actor = "nhà tuyển dụng phụ trách";
        String previous = null;
        try {
            var changes = history.findByApplicationIdOrderByCreatedAtAsc(application.getId());
            for (ApplicationStageHistory change : changes) {
                if (change.getCreatedAt() != null && notification.getCreatedAt() != null
                        && change.getCreatedAt().isAfter(notification.getCreatedAt().plusSeconds(30))) break;
                String changedTo = stageName(change.getToStageId());
                if (target.equalsIgnoreCase(changedTo)) {
                    actor = actorName(change.getActorId(), change.getActorName());
                    previous = stageName(change.getFromStageId());
                }
            }
        } catch (RuntimeException ignored) {
            // Preserve the translated message when historical details are unavailable.
        }
        return actor + " đã chuyển hồ sơ ứng tuyển của bạn cho vị trí " + jobName(application)
                + (previous != null ? " từ bước " + vietnameseStage(previous) : "")
                + " sang bước " + vietnameseStage(target) + ".";
    }

    private Application application(Notification notification) {
        if (notification.getEntityId() == null) return null;
        return applications.findOneById(notification.getEntityId()).orElse(null);
    }

    private String candidateName(Application application) {
        try {
            var cv = internalClient.getCv(application.getCvId().getLeastSignificantBits(), application.getCandidateId());
            if (cv != null && cv.candidateName() != null && !cv.candidateName().isBlank()
                    && !"New Candidate".equalsIgnoreCase(cv.candidateName())) return cv.candidateName();
        } catch (RuntimeException ignored) {
        }
        return actorName(application.getCandidateId(), application.getCandidateId().toString());
    }

    private String actorName(UUID actorId, String fallback) {
        if (actorId == null) return fallback;
        try {
            var identity = internalClient.getUserIdentity(actorId);
            if (identity != null) {
                if (identity.fullName() != null && !identity.fullName().isBlank()) return identity.fullName();
                if (identity.email() != null && !identity.email().isBlank()) return identity.email();
            }
        } catch (RuntimeException ignored) {
        }
        return fallback != null && !fallback.isBlank() ? fallback : actorId.toString();
    }

    private String jobName(Application application) {
        long jobId = application.getJobId().getLeastSignificantBits();
        try {
            var job = internalClient.getJobStatus(jobId);
            if (job != null && job.title() != null && !job.title().isBlank()) return "\"" + job.title() + "\"";
        } catch (RuntimeException ignored) {
        }
        return "tin tuyển dụng #" + jobId;
    }

    private String stageName(UUID stageId) {
        return stageId == null ? null : stages.findById(stageId).map(stage -> stage.getName()).orElse(null);
    }

    private String vietnameseStage(String name) {
        if (name == null || name.isBlank()) return "Chưa xác định";
        return switch (name.toLowerCase(java.util.Locale.ROOT)) {
            case "new" -> "Mới ứng tuyển";
            case "screening" -> "Sàng lọc";
            case "interviewing" -> "Phỏng vấn";
            case "offered" -> "Mời nhận việc";
            case "hired" -> "Đã tuyển";
            case "rejected" -> "Từ chối";
            default -> name;
        };
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        return repository.countByRecipientUserIdAndReadFalseAndCreatedAtAfter(
                RequestContext.userId(), Instant.now().minus(90, ChronoUnit.DAYS));
    }

    @Transactional
    public Notification markRead(UUID id) {
        Notification notification = repository.findByIdAndRecipientUserId(id, RequestContext.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ERR_NOTIFICATION_NOT_FOUND",
                        "Notification was not found"));
        notification.setRead(true);
        notification.setReadAt(Instant.now());
        return repository.save(notification);
    }

    @Transactional
    public int markAllRead() {
        return repository.markAllRead(RequestContext.userId(), Instant.now());
    }
}
