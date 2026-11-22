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
        if (type == null) return localizeUnknown(localized);
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
            case "INTERVIEW_SLOTS_PROPOSED" -> {
                localized.setTitle("Vui lòng chọn khung giờ phỏng vấn");
                localized.setMessage(original.getMessage() == null || original.getMessage().isBlank()
                        ? "Nhà tuyển dụng đã đề xuất các khung giờ phỏng vấn. Vui lòng vào mục Lịch phỏng vấn để chọn một khung phù hợp."
                        : original.getMessage());
            }
            case "INTERVIEW_SLOT_SELECTED" -> {
                localized.setTitle(original.getTitle() == null || original.getTitle().isBlank()
                        ? "Ứng viên đã chọn khung giờ phỏng vấn" : original.getTitle());
                localized.setMessage(original.getMessage() == null || original.getMessage().isBlank()
                        ? "Khung giờ phỏng vấn đã được xác nhận." : original.getMessage());
            }
            case "INTERVIEW_SLOT_CONFLICT" -> {
                localized.setTitle("Khung giờ phỏng vấn bị trùng");
                localized.setMessage(original.getMessage() == null || original.getMessage().isBlank()
                        ? "Khung giờ bạn chọn trùng với một lịch phỏng vấn khác. Vui lòng chọn khung giờ khác."
                        : original.getMessage());
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
            case "COMPANY_REGISTRATION_SUBMITTED" -> {
                localized.setTitle("Doanh nghiệp mới chờ duyệt");
                if (original.getMessage() == null || original.getMessage().matches("(?i).*pending approval.*")) {
                    localized.setMessage("Có hồ sơ đăng ký doanh nghiệp mới đang chờ SYS_ADMIN xem xét và phê duyệt.");
                }
            }
            default -> {
                return localizeUnknown(localized);
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
        if (application == null) {
            return "Hồ sơ ứng tuyển của bạn đang được nhà tuyển dụng xem xét.";
        }
        ApplicationStageHistory matchedChange = null;
        long nearestChangeSeconds = Long.MAX_VALUE;
        try {
            var changes = history.findByApplicationIdOrderByCreatedAtAsc(application.getId());
            for (ApplicationStageHistory change : changes) {
                if (change.getCreatedAt() == null || notification.getCreatedAt() == null) continue;
                long seconds = Math.abs(ChronoUnit.SECONDS.between(change.getCreatedAt(), notification.getCreatedAt()));
                if (seconds <= 120 && seconds < nearestChangeSeconds) {
                    nearestChangeSeconds = seconds;
                    matchedChange = change;
                }
            }
        } catch (RuntimeException ignored) {
            // Keep the event message if the audit history is unavailable.
        }

        if (matchedChange != null) {
            String target = stageName(matchedChange.getToStageId());
            String actor = actorName(matchedChange.getActorId(), matchedChange.getActorName());
            return candidateStageMessage(application, target, actor, matchedChange.getReason());
        }

        String original = notification.getMessage();
        if (original != null && !original.isBlank()
                && !original.startsWith("Your application moved to ")
                && !original.contains("đã được chuyển sang bước tiếp theo")
                && !original.toLowerCase(java.util.Locale.ROOT).contains("stage transition from ")) {
            return original;
        }
        return candidateStageMessage(application,
                application.getCurrentStage() == null ? null : application.getCurrentStage().getName(), null,
                application.getRejectionReason());
    }

    private String candidateStageMessage(Application application, String stage, String actor, String reason) {
        String job = jobName(application);
        String recruiter = actor == null || actor.isBlank() ? "Nhà tuyển dụng" : "Nhà tuyển dụng " + actor;
        if (stage == null || stage.isBlank()) {
            return "Hồ sơ ứng tuyển của bạn cho vị trí " + job + " đang được nhà tuyển dụng xem xét.";
        }
        String normalizedStage = stage.toLowerCase(java.util.Locale.ROOT);
        String message = switch (normalizedStage) {
            case "new" -> "Hồ sơ ứng tuyển của bạn cho vị trí " + job + " đã được tiếp nhận.";
            case "screening" -> recruiter + " đang xem xét hồ sơ của bạn cho vị trí " + job
                    + " ở vòng sàng lọc.";
            case "interviewing" -> "Hồ sơ của bạn đã qua vòng sàng lọc và đang ở giai đoạn phỏng vấn cho vị trí "
                    + job + ". Nhà tuyển dụng sẽ cập nhật lịch phỏng vấn riêng.";
            case "offered" -> "Hồ sơ của bạn đã đến giai đoạn đề xuất nhận việc cho vị trí " + job
                    + ". Vui lòng theo dõi mục Lời mời nhận việc để xem thông tin tiếp theo.";
            case "hired" -> "Chúc mừng! Nhà tuyển dụng đã xác nhận bạn trúng tuyển vị trí " + job + ".";
            case "rejected" -> "Rất tiếc, hồ sơ ứng tuyển của bạn cho vị trí " + job
                    + " chưa được tiếp tục.";
            default -> recruiter + " đang xem xét hồ sơ của bạn cho vị trí " + job
                    + " ở giai đoạn " + vietnameseStage(stage) + ".";
        };
        String visibleReason = candidateVisibleReason(reason);
        if (visibleReason == null) return message;
        String label = "rejected".equals(normalizedStage) ? "Lý do: " : "Ghi chú từ nhà tuyển dụng: ";
        return message + " " + label + visibleReason;
    }

    private String candidateVisibleReason(String reason) {
        if (reason == null || reason.isBlank()) return null;
        String value = reason.trim();
        String normalized = value.toLowerCase(java.util.Locale.ROOT);
        if (normalized.startsWith("stage transition from ")
                || normalized.equals("candidate withdrew application")
                || normalized.equals("offer expired")) {
            return null;
        }
        return value;
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
        String name = actorName(application.getCandidateId(), null);
        return name != null ? name : "ứng viên";
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
        return fallback != null && !fallback.isBlank() ? fallback : null;
    }

    private String jobName(Application application) {
        long jobId = application.getJobId().getLeastSignificantBits();
        try {
            var job = internalClient.getJobStatus(jobId);
            if (job != null && job.title() != null && !job.title().isBlank()) return "\"" + job.title() + "\"";
        } catch (RuntimeException ignored) {
        }
        return "vị trí tuyển dụng";
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

    private Notification localizeUnknown(Notification notification) {
        String type = notification.getActionType();
        if (type != null && type.startsWith("JOB_")) {
            notification.setTitle("Cập nhật tin tuyển dụng");
            notification.setMessage("Tin tuyển dụng đã có thay đổi. Vui lòng mở trang quản lý tin tuyển dụng để xem chi tiết.");
        } else if (type != null && type.startsWith("OFFER_")) {
            notification.setTitle("Cập nhật lời mời nhận việc");
            notification.setMessage("Lời mời nhận việc đã có thay đổi. Vui lòng mở mục Lời mời nhận việc để xem chi tiết.");
        } else if (type != null && type.startsWith("INTERVIEW_")) {
            notification.setTitle("Cập nhật lịch phỏng vấn");
            notification.setMessage("Lịch phỏng vấn đã có thay đổi. Vui lòng mở mục Lịch phỏng vấn để xem chi tiết.");
        } else if (type != null && type.startsWith("APPLICATION_")) {
            notification.setTitle("Cập nhật hồ sơ ứng tuyển");
            notification.setMessage("Hồ sơ ứng tuyển đã có thay đổi. Vui lòng mở mục Quy trình ATS để xem chi tiết.");
        } else {
            notification.setTitle("Thông báo mới");
            notification.setMessage("Hệ thống có cập nhật mới. Vui lòng mở ứng dụng để xem chi tiết.");
        }
        return notification;
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
