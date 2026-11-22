package com.rms.ats.service;

import static com.rms.ats.exception.ApiExceptionHandler.ApiException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rms.ats.dto.ApplicationDtos.ApplicationResponse;
import com.rms.ats.dto.ApplicationDtos.HistoryResponse;
import com.rms.ats.dto.ApplicationDtos.MoveStageRequest;
import com.rms.ats.dto.ApplicationDtos.PageResponse;
import com.rms.ats.dto.ApplicationDtos.StageColumn;
import com.rms.ats.dto.ApplicationDtos.SubmitApplicationRequest;
import com.rms.ats.config.RequestContext;
import com.rms.ats.entity.Application;
import com.rms.ats.entity.ApplicationStageHistory;
import com.rms.ats.entity.ApplicationStatus;
import com.rms.ats.entity.AuditLog;
import com.rms.ats.entity.IdempotencyKey;
import com.rms.ats.entity.PipelineStage;
import com.rms.ats.client.InternalServiceClient;
import com.rms.ats.messaging.NotificationEvent;
import com.rms.ats.messaging.NotificationPublisher;
import com.rms.ats.repository.ApplicationRepository;
import com.rms.ats.repository.ApplicationStageHistoryRepository;
import com.rms.ats.repository.AuditLogRepository;
import com.rms.ats.repository.IdempotencyKeyRepository;
import com.rms.ats.repository.PipelineStageRepository;

@Service
public class ApplicationService {

    private final ApplicationRepository applications;
    private final PipelineStageRepository stages;
    private final ApplicationStageHistoryRepository history;
    private final AuditService auditService;
    private final IdempotencyKeyRepository idempotencyKeys;
    private final InternalServiceClient internalClient;
    private final NotificationPublisher notifications;
    private final ObjectMapper objectMapper;

    public ApplicationService(
            ApplicationRepository applications,
            PipelineStageRepository stages,
            ApplicationStageHistoryRepository history,
            AuditService auditService,
            IdempotencyKeyRepository idempotencyKeys,
            InternalServiceClient internalClient,
            NotificationPublisher notifications,
            ObjectMapper objectMapper) {
        this.applications = applications;
        this.stages = stages;
        this.history = history;
        this.auditService = auditService;
        this.idempotencyKeys = idempotencyKeys;
        this.internalClient = internalClient;
        this.notifications = notifications;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ApplicationResponse submit(SubmitApplicationRequest request, String idempotencyKey) {
        requireRole("CANDIDATE");
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ERR_IDEMPOTENCY_KEY_REQUIRED",
                    "Idempotency-Key header is required");
        }
        try {
            UUID.fromString(idempotencyKey);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ERR_IDEMPOTENCY_KEY_INVALID",
                    "Idempotency-Key must be a UUID");
        }
        UUID candidateId = RequestContext.userId();
        if (request.jobId() <= 0 || request.cvId() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ERR_INVALID_RESOURCE_ID",
                    "Job and CV IDs must be positive numbers");
        }
        String scopedKey = candidateId + ":SUBMIT_APPLICATION:" + idempotencyKey;
        var existingKey = idempotencyKeys.findById(scopedKey);
        if (existingKey.isPresent()) {
            return ApplicationResponse.from(applications.findOneById(existingKey.get().getResourceId())
                    .filter(application -> application.getCandidateId().equals(candidateId))
                    .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "ERR_IDEMPOTENCY_STATE",
                            "Stored idempotency result is unavailable")));
        }

        var job = internalClient.getJobStatus(request.jobId());
        if (job == null || job.tenantId() == null || !"PUBLISHED".equals(job.status())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ERR_JOB_NOT_PUBLISHED",
                    "Application can only be submitted to a published job");
        }
        UUID tenantId = job.tenantId();
        var cv = internalClient.getCv(request.cvId(), candidateId);
        if (cv == null || !cv.valid() || !request.cvId().equals(cv.cvId())
                || !candidateId.equals(cv.candidateId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ERR_CV_NOT_OWNED",
                    "The selected CV does not belong to the candidate");
        }
        UUID storedJobId = new UUID(0L, request.jobId());
        UUID storedCvId = new UUID(1L, request.cvId());
        if (applications.existsByJobIdAndCandidateId(storedJobId, candidateId)) {
            throw new ApiException(HttpStatus.CONFLICT, "ERR_DUPLICATE_APPLICATION",
                    "Candidate has already applied to this job");
        }

        PipelineStage firstStage = ensureDefaultStages(tenantId, storedJobId);
        Application application = new Application();
        application.setTenantId(tenantId);
        application.setJobId(storedJobId);
        application.setCandidateId(candidateId);
        application.setCvId(storedCvId);
        application.setCoverLetter(request.coverLetter());
        application.setCurrentStage(firstStage);
        try {
            application = applications.saveAndFlush(application);
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "ERR_DUPLICATE_APPLICATION",
                    "Candidate has already applied to this job");
        }

        recordHistory(application, null, firstStage.getId(), candidateId, "Application submitted");
        idempotencyKeys.save(new IdempotencyKey(scopedKey, "SUBMIT_APPLICATION", application.getId()));
        
        List<UUID> recruiterIds = internalClient.getTenantRecruiterIds(tenantId);
        if (recruiterIds.isEmpty() && job.recruiterId() != null) {
            recruiterIds = List.of(job.recruiterId());
        }
        for (UUID recruiterId : recruiterIds) {
            notifications.publish(new NotificationEvent(tenantId, recruiterId,
                    "APPLICATION_SUBMITTED", "APPLICATION", application.getId(),
                    "Có hồ sơ ứng tuyển mới",
                    "Ứng viên " + candidateName(cv.candidateName(), candidateId)
                            + " đã ứng tuyển vào vị trí " + jobName(job.title(), request.jobId()) + "."));
        }
        return ApplicationResponse.from(application);
    }

    @Transactional(readOnly = true)
    public ApplicationResponse get(UUID id) {
        Application application = requireReadableApplication(id);
        String role = RequestContext.role().replaceFirst("(?i)^ROLE_", "");
        return "HR_ADMIN".equalsIgnoreCase(role) || "RECRUITER".equalsIgnoreCase(role)
                ? employerApplicationResponse(application)
                : ApplicationResponse.from(application);
    }

    @Transactional(readOnly = true)
    public List<StageColumn> kanban(UUID jobId) {
        UUID tenantId = requireEmployerTenant();
        List<PipelineStage> jobStages = stages.findByJobIdAndTenantIdOrderBySequenceOrder(jobId, tenantId);
        return jobStages.stream()
                .map(stage -> {
                    Page<Application> page = applications
                            .findByJobIdAndTenantIdAndCurrentStageIdOrderByAppliedAtDesc(
                                    jobId, tenantId, stage.getId(), PageRequest.of(0, 50));
                    return new StageColumn(stage.getId(), stage.getName(), stage.getSequenceOrder(),
                            stage.isTerminal(), page.getTotalElements(), page.hasNext(),
                            page.getContent().stream().map(this::employerApplicationResponse).toList());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public CvDocument employerCv(UUID applicationId) {
        Application application = requireEmployerApplication(applicationId);
        var cv = internalClient.getCv(application.getCvId().getLeastSignificantBits(), application.getCandidateId());
        if (cv == null || !cv.valid() || !application.getCandidateId().equals(cv.candidateId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ERR_CV_NOT_FOUND", "Application CV was not found");
        }
        try {
            byte[] content = internalClient.downloadCv(application.getCvId().getLeastSignificantBits());
            if (content == null || content.length == 0) {
                throw new ApiException(HttpStatus.NOT_FOUND, "ERR_CV_NOT_FOUND", "Application CV file is empty");
            }
            String contentType = safeCvContentType(cv.contentType(), cv.fileName());
            boolean pdf = "application/pdf".equals(contentType);
            return new CvDocument(content, contentType, safeFileName(cv.fileName()), pdf);
        } catch (ApiException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "ERR_CV_DOWNLOAD_FAILED",
                    "Could not retrieve the application CV");
        }
    }

    private String safeCvContentType(String contentType, String fileName) {
        if (contentType != null && ("application/pdf".equalsIgnoreCase(contentType)
                || "application/msword".equalsIgnoreCase(contentType)
                || "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equalsIgnoreCase(contentType))) {
            return contentType;
        }
        String name = fileName == null ? "" : fileName.toLowerCase(java.util.Locale.ROOT);
        if (name.endsWith(".pdf")) return "application/pdf";
        if (name.endsWith(".doc")) return "application/msword";
        if (name.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        return "application/octet-stream";
    }

    private String safeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) return "candidate-cv";
        return fileName.replaceAll("[\\\\/\\r\\n\\\"]", "_");
    }

    public record CvDocument(byte[] content, String contentType, String fileName, boolean inline) {
    }

    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> stageApplications(
            UUID jobId, UUID stageId, int page, int size) {
        UUID tenantId = requireEmployerTenant();
        PipelineStage stage = stages.findByIdAndJobIdAndTenantId(stageId, jobId, tenantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ERR_STAGE_NOT_FOUND",
                        "Pipeline stage was not found for this job"));
        Page<Application> result = applications
                .findByJobIdAndTenantIdAndCurrentStageIdOrderByAppliedAtDesc(
                        jobId, tenantId, stage.getId(), PageRequest.of(normalizePage(page), normalizeSize(size, 50)));
        return pageResponse(result, true);
    }

    @Transactional
    public ApplicationResponse moveStage(
            UUID applicationId,
            MoveStageRequest request,
            String ipAddress,
            String deviceInfo) {
        Application application = requireEmployerApplication(applicationId);
        if (application.isTerminal()) {
            throw new ApiException(HttpStatus.CONFLICT, "ERR_TERMINAL_APPLICATION",
                    "Terminal applications cannot be moved");
        }

        PipelineStage target = stages.findByIdAndJobIdAndTenantId(
                        request.newStageId(), application.getJobId(), application.getTenantId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ERR_STAGE_NOT_FOUND",
                        "Pipeline stage was not found for this job"));
        if (target.getTerminalStatus() == ApplicationStatus.REJECTED
                && (request.reason() == null || request.reason().isBlank())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ERR_REJECTION_REASON_REQUIRED",
                    "A rejection reason is required");
        }

        PipelineStage previous = application.getCurrentStage();
        Map<String, Object> before = Map.of(
                "stageId", previous.getId(),
                "stageName", previous.getName(),
                "status", application.getStatus());

        application.setCurrentStage(target);
        application.setUpdatedAt(Instant.now());
        if (target.isTerminal()) {
            application.setStatus(target.getTerminalStatus());
            if (target.getTerminalStatus() == ApplicationStatus.REJECTED) {
                application.setRejectionReason(request.reason());
            }
        }
        applications.save(application);
        recordHistory(application, previous.getId(), target.getId(), RequestContext.userId(), request.reason());
        writeAudit(application, "APPLICATION_STAGE_CHANGED", before, ipAddress, deviceInfo, request.reason());

        notifications.publish(new NotificationEvent(application.getTenantId(), application.getCandidateId(),
                "APPLICATION_STAGE_CHANGED", "APPLICATION", application.getId(),
                "Cập nhật quy trình ứng tuyển",
                candidateStageMessage(application, target, actorName(RequestContext.userId()), request.reason())));
        return ApplicationResponse.from(application);
    }

    @Transactional
    public ApplicationResponse withdraw(UUID id, String reason, String ipAddress, String deviceInfo) {
        requireRole("CANDIDATE");
        Application application = applications.findOneById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ERR_APPLICATION_NOT_FOUND",
                        "Application was not found"));
        if (!application.getCandidateId().equals(RequestContext.userId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ERR_APPLICATION_NOT_OWNED",
                    "Candidate does not own this application");
        }
        if (application.isTerminal()) {
            throw new ApiException(HttpStatus.CONFLICT, "ERR_TERMINAL_APPLICATION",
                    "Only active applications can be withdrawn");
        }
        Map<String, Object> before = Map.of(
                "stageId", application.getCurrentStage().getId(),
                "status", application.getStatus());
        application.setStatus(ApplicationStatus.WITHDRAWN);
        application.setUpdatedAt(Instant.now());
        applications.save(application);
        recordHistory(application, application.getCurrentStage().getId(), application.getCurrentStage().getId(),
                RequestContext.userId(), reason != null ? reason : "Candidate withdrew application");
        writeAudit(application, "APPLICATION_WITHDRAWN", before, ipAddress, deviceInfo, reason);

        var job = internalClient.getJobStatus(application.getJobId());
        if (job != null && job.recruiterId() != null) {
            notifications.publish(new NotificationEvent(application.getTenantId(), job.recruiterId(),
                    "APPLICATION_WITHDRAWN", "APPLICATION", application.getId(),
                    "Ứng viên đã rút hồ sơ",
                    "Ứng viên " + candidateName(application) + " đã rút hồ sơ ứng tuyển khỏi vị trí "
                            + jobName(job.title(), application.getJobId().getLeastSignificantBits()) + "."));
        }
        return ApplicationResponse.from(application);
    }

    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> candidateApplications(int page, int size) {
        requireRole("CANDIDATE");
        Page<Application> result = applications.findByCandidateIdOrderByAppliedAtDesc(
                RequestContext.userId(), PageRequest.of(normalizePage(page), normalizeSize(size, 20)));
        return pageResponse(result);
    }

    @Transactional(readOnly = true)
    public List<UUID> candidateApplicationIds(UUID candidateId) {
        return applications.findApplicationIdsByCandidateId(candidateId);
    }

    @Transactional(readOnly = true)
    public List<HistoryResponse> stageHistory(UUID id) {
        requireReadableApplication(id);
        var items = history.findByApplicationIdOrderByCreatedAtAsc(id);
        Set<UUID> stageIds = items.stream()
                .flatMap(item -> java.util.stream.Stream.of(item.getFromStageId(), item.getToStageId()))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, PipelineStage> stageById = stages.findAllById(stageIds).stream()
                .collect(Collectors.toMap(PipelineStage::getId, Function.identity()));
        boolean candidateView = "CANDIDATE".equals(RequestContext.role());
        return items.stream()
                .map(item -> new HistoryResponse(
                        item.getId(),
                        item.getFromStageId(), stageName(stageById, item.getFromStageId()),
                        item.getToStageId(), stageName(stageById, item.getToStageId()),
                        candidateView ? null : item.getActorId(),
                        candidateView ? null : (item.getActorName() != null ? item.getActorName() : (item.getActorId() != null ? item.getActorId().toString() : "System")),
                        candidateView ? null : item.getReason(),
                        item.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getInternal(UUID id) {
        Application application = applications.findOneById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ERR_APPLICATION_NOT_FOUND",
                        "Application was not found"));
        return employerApplicationResponse(application);
    }

    @Transactional
    public ApplicationResponse hireInternal(UUID id, UUID tenantId) {
        return hireInternal(id, tenantId, null);
    }

    @Transactional
    public ApplicationResponse hireInternal(UUID id, UUID tenantId, UUID explicitActorId) {
        Application application;
        if (tenantId != null) {
            application = applications.findByIdAndTenantId(id, tenantId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ERR_APPLICATION_NOT_FOUND",
                            "Application was not found for the given tenant"));
        } else {
            application = applications.findOneById(id)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ERR_APPLICATION_NOT_FOUND",
                            "Application was not found"));
        }

        if (application.getStatus() == ApplicationStatus.HIRED) {
            return ApplicationResponse.from(application);
        }
        if (application.getStatus() == ApplicationStatus.REJECTED || application.getStatus() == ApplicationStatus.WITHDRAWN) {
            throw new ApiException(HttpStatus.CONFLICT, "ERR_TERMINAL_APPLICATION",
                    "Cannot hire an application that is already " + application.getStatus());
        }

        UUID appTenantId = application.getTenantId();
        UUID jobId = application.getJobId();
        PipelineStage hiredStage = stages.findFirstByJobIdAndTenantIdAndTerminalStatus(jobId, appTenantId, ApplicationStatus.HIRED)
                .or(() -> stages.findFirstByJobIdAndTenantIdAndNameIgnoreCase(jobId, appTenantId, "Hired"))
                .orElseGet(() -> {
                    int maxOrder = stages.findByJobIdAndTenantIdOrderBySequenceOrder(jobId, appTenantId).stream()
                            .mapToInt(PipelineStage::getSequenceOrder)
                            .max()
                            .orElse(4);
                    PipelineStage stage = new PipelineStage(appTenantId, jobId, "Hired", maxOrder + 1, true, ApplicationStatus.HIRED);
                    return stages.save(stage);
                });

        PipelineStage previous = application.getCurrentStage();
        UUID previousStageId = previous != null ? previous.getId() : null;
        String previousStageName = previous != null ? previous.getName() : null;

        Map<String, Object> before = Map.of(
                "stageId", previousStageId != null ? previousStageId.toString() : "",
                "stageName", previousStageName != null ? previousStageName : "",
                "status", application.getStatus().name());

        application.setCurrentStage(hiredStage);
        application.setStatus(ApplicationStatus.HIRED);
        application.setUpdatedAt(Instant.now());
        applications.save(application);

        UUID actorId = explicitActorId != null ? explicitActorId : (RequestContext.currentUserId() != null ? RequestContext.currentUserId() : application.getCandidateId());
        recordHistory(application, previousStageId, hiredStage.getId(), actorId, "Candidate hired via offer acceptance");

        AuditLog audit = new AuditLog();
        audit.setTenantId(appTenantId);
        audit.setActorId(actorId);
        audit.setActionType("APPLICATION_HIRED");
        audit.setEntityType("APPLICATION");
        audit.setEntityId(application.getId());
        audit.setBeforeState(objectMapper.valueToTree(before));
        audit.setAfterState(objectMapper.valueToTree(Map.of(
                "stageId", hiredStage.getId().toString(),
                "stageName", hiredStage.getName(),
                "status", application.getStatus().name())));
        audit.setIpAddress("INTERNAL");
        audit.setDeviceInfo("INTERNAL_SERVICE");
        auditService.save(audit);

        var job = internalClient.getJobStatus(jobId);
        if (job != null && job.recruiterId() != null) {
            notifications.publish(new NotificationEvent(appTenantId, job.recruiterId(),
                    "APPLICATION_HIRED", "APPLICATION", application.getId(),
                    "Ứng viên đã nhận việc",
                    "Ứng viên " + candidateName(application) + " đã chấp nhận lời mời nhận việc cho vị trí "
                            + jobName(job.title(), jobId.getLeastSignificantBits()) + " và được chuyển sang trạng thái Đã tuyển."));
        }

        return ApplicationResponse.from(application);
    }

    @Transactional
    public void handleOfferExpired(UUID offerId, UUID applicationId, UUID tenantId) {
        Application application;
        if (tenantId != null) {
            application = applications.findByIdAndTenantId(applicationId, tenantId).orElse(null);
        } else {
            application = applications.findOneById(applicationId).orElse(null);
        }

        if (application == null) {
            return;
        }

        if (application.getStatus() == ApplicationStatus.HIRED
                || application.getStatus() == ApplicationStatus.REJECTED
                || application.getStatus() == ApplicationStatus.WITHDRAWN) {
            return;
        }

        UUID appTenantId = application.getTenantId();
        UUID jobId = application.getJobId();
        PipelineStage rejectedStage = stages.findFirstByJobIdAndTenantIdAndTerminalStatus(jobId, appTenantId, ApplicationStatus.REJECTED)
                .or(() -> stages.findFirstByJobIdAndTenantIdAndNameIgnoreCase(jobId, appTenantId, "Rejected"))
                .orElseGet(() -> {
                    int maxOrder = stages.findByJobIdAndTenantIdOrderBySequenceOrder(jobId, appTenantId).stream()
                            .mapToInt(PipelineStage::getSequenceOrder)
                            .max()
                            .orElse(5);
                    PipelineStage stage = new PipelineStage(appTenantId, jobId, "Rejected", maxOrder + 1, true, ApplicationStatus.REJECTED);
                    return stages.save(stage);
                });

        PipelineStage previous = application.getCurrentStage();
        UUID previousStageId = previous != null ? previous.getId() : null;
        String previousStageName = previous != null ? previous.getName() : null;

        Map<String, Object> before = Map.of(
                "stageId", previousStageId != null ? previousStageId.toString() : "",
                "stageName", previousStageName != null ? previousStageName : "",
                "status", application.getStatus().name());

        application.setCurrentStage(rejectedStage);
        application.setStatus(ApplicationStatus.REJECTED);
        application.setRejectionReason("Offer expired");
        application.setUpdatedAt(Instant.now());
        applications.save(application);

        recordHistory(application, previousStageId, rejectedStage.getId(), null, "Offer expired");

        AuditLog audit = new AuditLog();
        audit.setTenantId(appTenantId);
        audit.setActorId(null);
        audit.setActionType("OFFER_EXPIRED");
        audit.setEntityType("APPLICATION");
        audit.setEntityId(application.getId());
        audit.setBeforeState(objectMapper.valueToTree(before));
        audit.setAfterState(objectMapper.valueToTree(Map.of(
                "stageId", rejectedStage.getId().toString(),
                "stageName", rejectedStage.getName(),
                "status", application.getStatus().name(),
                "rejectionReason", "Offer expired")));
        audit.setIpAddress("INTERNAL");
        audit.setDeviceInfo("INTERNAL_SCHEDULER");
        auditService.save(audit);

        // Notify recruiters and HR Admin
        List<UUID> recruiterIds = new java.util.ArrayList<>(internalClient.getTenantRecruiterIds(appTenantId));
        var job = internalClient.getJobStatus(jobId);
        if (job != null && job.recruiterId() != null && !recruiterIds.contains(job.recruiterId())) {
            recruiterIds.add(job.recruiterId());
        }
        for (UUID recruiterId : recruiterIds) {
            notifications.publish(new NotificationEvent(appTenantId, recruiterId,
                    "OFFER_EXPIRED", "APPLICATION", application.getId(),
                    "Lời mời nhận việc đã hết hạn",
                    "Lời mời nhận việc gửi cho ứng viên " + candidateName(application)
                            + " ở vị trí " + jobName(job != null ? job.title() : null, jobId.getLeastSignificantBits())
                            + " đã hết hạn. Hồ sơ được chuyển sang bước Từ chối."));
        }
    }

    @Transactional(readOnly = true)
    public boolean validateInternal(UUID id, UUID tenantId) {
        var opt = tenantId != null ? applications.findByIdAndTenantId(id, tenantId) : applications.findOneById(id);
        if (opt.isEmpty()) {
            return false;
        }
        Application app = opt.get();
        return app.getStatus() == ApplicationStatus.ACTIVE
                && app.getCurrentStage() != null
                && "Offered".equalsIgnoreCase(app.getCurrentStage().getName());
    }

    @Transactional(readOnly = true)
    public boolean isCvInUse(Long cvId) {
        return applications.existsByCvIdAndStatus(new UUID(1L, cvId), ApplicationStatus.ACTIVE);
    }

    @Transactional
    public void createDefaultStagesForJob(UUID tenantId, UUID jobId) {
        ensureDefaultStages(tenantId, jobId);
    }

    private Application requireReadableApplication(UUID id) {
        Application application = applications.findOneById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ERR_APPLICATION_NOT_FOUND",
                        "Application was not found"));
        if ("CANDIDATE".equals(RequestContext.role())) {
            if (!application.getCandidateId().equals(RequestContext.userId())) {
                throw new ApiException(HttpStatus.NOT_FOUND, "ERR_APPLICATION_NOT_FOUND",
                        "Application was not found");
            }
            return application;
        }
        requireEmployerRole();
        if (!application.getTenantId().equals(RequestContext.tenantId())) {
            UUID tenantId = RequestContext.tenantId() != null ? RequestContext.tenantId() : application.getTenantId();
            UUID actorId = RequestContext.userId();
            auditService.logCrossTenantAttempt(tenantId, actorId, application.getId());

            throw new ApiException(HttpStatus.FORBIDDEN, "ERR_TENANT_MISMATCH",
                    "Application belongs to another tenant");
        }
        return application;
    }

    private Application requireEmployerApplication(UUID id) {
        UUID tenantId = requireEmployerTenant();
        return applications.findByIdAndTenantId(id, tenantId)
                .orElseGet(() -> {
                    applications.findOneById(id).ifPresent(app -> {
                        if (!tenantId.equals(app.getTenantId())) {
                            UUID actorId = RequestContext.userId();
                            auditService.logCrossTenantAttempt(tenantId, actorId, app.getId());
                            throw new ApiException(HttpStatus.FORBIDDEN, "ERR_TENANT_MISMATCH",
                                    "Application belongs to another tenant");
                        }
                    });
                    throw new ApiException(HttpStatus.NOT_FOUND, "ERR_APPLICATION_NOT_FOUND",
                            "Application was not found");
                });
    }

    private UUID requireEmployerTenant() {
        requireEmployerRole();
        UUID tenantId = RequestContext.tenantId();
        if (tenantId == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ERR_TENANT_REQUIRED",
                    "Employer operations require a tenant");
        }
        return tenantId;
    }

    private void requireEmployerRole() {
        requireRole("RECRUITER", "HR_ADMIN");
    }

    private void requireRole(String... allowedRoles) {
        String currentRole = RequestContext.role().replaceFirst("(?i)^ROLE_", "");
        if (java.util.Arrays.stream(allowedRoles).noneMatch(role -> role.equalsIgnoreCase(currentRole))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ERR_INSUFFICIENT_PERMISSION",
                    "User role does not have permission for this operation");
        }
    }

    private int normalizePage(int page) {
        return Math.max(page, 0);
    }

    private int normalizeSize(int size, int defaultSize) {
        return size <= 0 ? defaultSize : Math.min(size, 50);
    }

    private PageResponse<ApplicationResponse> pageResponse(Page<Application> page) {
        return pageResponse(page, false);
    }

    private PageResponse<ApplicationResponse> pageResponse(Page<Application> page, boolean includeCandidateDetails) {
        return new PageResponse<>(page.getContent().stream()
                .map(application -> includeCandidateDetails
                        ? employerApplicationResponse(application)
                        : ApplicationResponse.from(application))
                .toList(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private ApplicationResponse employerApplicationResponse(Application application) {
        try {
            var cv = internalClient.getCv(application.getCvId().getLeastSignificantBits(), application.getCandidateId());
            if (cv != null && cv.valid() && application.getCandidateId().equals(cv.candidateId())) {
                String candidateName = cv.candidateName();
                String candidateEmail = cv.candidateEmail();
                if (isPlaceholder(candidateName) || isPlaceholder(candidateEmail)) {
                    var identity = internalClient.getUserIdentity(application.getCandidateId());
                    if (identity != null && "CANDIDATE".equalsIgnoreCase(identity.role())) {
                        if (isPlaceholder(candidateName)) {
                            candidateName = identity.fullName();
                        }
                        if (isPlaceholder(candidateEmail)) {
                            candidateEmail = identity.email();
                        }
                    }
                }
                if (isPlaceholder(candidateName)) {
                    candidateName = null;
                }
                if (isPlaceholder(candidateEmail)) {
                    candidateEmail = null;
                }
                return ApplicationResponse.from(application, candidateName, candidateEmail, cv.fileName());
            }
        } catch (RuntimeException ignored) {
            // Keep the application visible even when Candidate Service is temporarily unavailable.
        }
        return ApplicationResponse.from(application);
    }

    private boolean isPlaceholder(String value) {
        return value == null || value.isBlank()
                || "New Candidate".equalsIgnoreCase(value)
                || value.endsWith("@placeholder.local");
    }

    private String candidateName(Application application) {
        try {
            var cv = internalClient.getCv(application.getCvId().getLeastSignificantBits(), application.getCandidateId());
            if (cv != null && application.getCandidateId().equals(cv.candidateId())) {
                return candidateName(cv.candidateName(), application.getCandidateId());
            }
        } catch (RuntimeException ignored) {
            // A temporary profile-service failure must not cancel the ATS transition.
        }
        return candidateName(null, application.getCandidateId());
    }

    private String candidateName(String profileName, UUID candidateId) {
        if (!isPlaceholder(profileName)) return profileName.trim();
        try {
            var identity = internalClient.getUserIdentity(candidateId);
            if (identity != null) {
                if (!isPlaceholder(identity.fullName())) return identity.fullName().trim();
                if (!isPlaceholder(identity.email())) return identity.email();
            }
        } catch (RuntimeException ignored) {
            // Keep a useful identifier if IAM is temporarily unavailable.
        }
        return "ứng viên";
    }

    private String actorName(UUID actorId) {
        try {
            var identity = internalClient.getUserIdentity(actorId);
            if (identity != null) {
                if (!isPlaceholder(identity.fullName())) return identity.fullName().trim();
                if (!isPlaceholder(identity.email())) return identity.email();
            }
        } catch (RuntimeException ignored) {
            // Fall back to the request context if IAM is temporarily unavailable.
        }
        return RequestContext.userName();
    }

    private String jobName(UUID jobId) {
        try {
            var job = internalClient.getJobStatus(jobId);
            return jobName(job != null ? job.title() : null, jobId.getLeastSignificantBits());
        } catch (RuntimeException ignored) {
            return jobName(null, jobId.getLeastSignificantBits());
        }
    }

    private String jobName(String title, long jobId) {
        return title != null && !title.isBlank() ? "\"" + title.trim() + "\"" : "vị trí tuyển dụng";
    }

    private String stageNameVietnamese(String stageName) {
        if (stageName == null) return "Chưa xác định";
        return switch (stageName.toLowerCase(java.util.Locale.ROOT)) {
            case "new" -> "Mới ứng tuyển";
            case "screening" -> "Sàng lọc";
            case "interviewing" -> "Phỏng vấn";
            case "offered" -> "Mời nhận việc";
            case "hired" -> "Đã tuyển";
            case "rejected" -> "Từ chối";
            default -> "bước tiếp theo";
        };
    }

    private String candidateStageMessage(
            Application application, PipelineStage target, String actor, String reason) {
        String job = jobName(application.getJobId());
        String recruiter = actor == null || actor.isBlank() ? "Nhà tuyển dụng" : "Nhà tuyển dụng " + actor;
        String message = switch (target.getName().toLowerCase(java.util.Locale.ROOT)) {
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
                    + " ở giai đoạn " + target.getName() + ".";
        };
        String visibleReason = candidateVisibleReason(reason);
        if (visibleReason == null) return message;
        String label = "rejected".equalsIgnoreCase(target.getName()) ? "Lý do: " : "Ghi chú từ nhà tuyển dụng: ";
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

    private String stageName(Map<UUID, PipelineStage> stageById, UUID stageId) {
        return stageId == null || stageById.get(stageId) == null ? null : stageById.get(stageId).getName();
    }

    private PipelineStage ensureDefaultStages(UUID tenantId, UUID jobId) {
        return stages.findFirstByJobIdAndTenantIdOrderBySequenceOrder(jobId, tenantId)
                .orElseGet(() -> {
                    List<PipelineStage> defaults = List.of(
                            new PipelineStage(tenantId, jobId, "New", 1, false, null),
                            new PipelineStage(tenantId, jobId, "Screening", 2, false, null),
                            new PipelineStage(tenantId, jobId, "Interviewing", 3, false, null),
                            new PipelineStage(tenantId, jobId, "Offered", 4, false, null),
                            new PipelineStage(tenantId, jobId, "Hired", 5, true, ApplicationStatus.HIRED),
                            new PipelineStage(tenantId, jobId, "Rejected", 6, true, ApplicationStatus.REJECTED));
                    return stages.saveAll(defaults).get(0);
                });
    }

    private void recordHistory(
            Application application,
            UUID fromStageId,
            UUID toStageId,
            UUID actorId,
            String reason) {
        ApplicationStageHistory item = new ApplicationStageHistory();
        item.setApplication(application);
        item.setFromStageId(fromStageId);
        item.setToStageId(toStageId);
        item.setActorId(actorId);
        item.setActorName(RequestContext.userName());
        item.setReason(reason);
        history.save(item);
    }

    private void writeAudit(
            Application application,
            String actionType,
            Map<String, Object> before,
            String ipAddress,
            String deviceInfo,
            String reason) {
        AuditLog audit = new AuditLog();
        audit.setTenantId(application.getTenantId());
        audit.setActorId(RequestContext.userId());
        audit.setActionType(actionType);
        audit.setEntityType("APPLICATION");
        audit.setEntityId(application.getId());
        audit.setBeforeState(objectMapper.valueToTree(before));
        java.util.Map<String, Object> after = new java.util.HashMap<>();
        if (application.getCurrentStage() != null) {
            after.put("stageId", application.getCurrentStage().getId());
            after.put("stageName", application.getCurrentStage().getName());
        }
        after.put("status", application.getStatus());
        if (reason != null) {
            after.put("reason", reason);
        }
        audit.setAfterState(objectMapper.valueToTree(after));
        audit.setIpAddress(ipAddress);
        audit.setDeviceInfo(deviceInfo);
        auditService.save(audit);
    }

    @Transactional
    public void createDefaultStagesForJob(Object rawJobId, UUID tenantId) {
        if (tenantId == null || rawJobId == null) {
            return;
        }
        UUID jobId;
        if (rawJobId instanceof UUID u) {
            jobId = u;
        } else if (rawJobId instanceof Long l) {
            jobId = new UUID(0L, l);
        } else {
            String value = String.valueOf(rawJobId);
            try {
                jobId = new UUID(0L, Long.parseLong(value));
            } catch (NumberFormatException notNumeric) {
                try {
                    jobId = UUID.fromString(value);
                } catch (IllegalArgumentException notUuid) {
                    jobId = UUID.nameUUIDFromBytes(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
            }
        }
        ensureDefaultStages(tenantId, jobId);
    }
}
