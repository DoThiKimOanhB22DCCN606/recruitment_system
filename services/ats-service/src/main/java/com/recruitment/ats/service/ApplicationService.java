package com.recruitment.ats.service;

import static com.recruitment.ats.api.ApiExceptionHandler.ApiException;

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
import com.recruitment.ats.api.ApplicationDtos.ApplicationResponse;
import com.recruitment.ats.api.ApplicationDtos.HistoryResponse;
import com.recruitment.ats.api.ApplicationDtos.MoveStageRequest;
import com.recruitment.ats.api.ApplicationDtos.PageResponse;
import com.recruitment.ats.api.ApplicationDtos.StageColumn;
import com.recruitment.ats.api.ApplicationDtos.SubmitApplicationRequest;
import com.recruitment.ats.config.RequestContext;
import com.recruitment.ats.domain.Application;
import com.recruitment.ats.domain.ApplicationStageHistory;
import com.recruitment.ats.domain.ApplicationStatus;
import com.recruitment.ats.domain.AuditLog;
import com.recruitment.ats.domain.IdempotencyKey;
import com.recruitment.ats.domain.PipelineStage;
import com.recruitment.ats.integration.InternalServiceClient;
import com.recruitment.ats.messaging.NotificationEvent;
import com.recruitment.ats.messaging.NotificationPublisher;
import com.recruitment.ats.repository.ApplicationRepository;
import com.recruitment.ats.repository.ApplicationStageHistoryRepository;
import com.recruitment.ats.repository.AuditLogRepository;
import com.recruitment.ats.repository.IdempotencyKeyRepository;
import com.recruitment.ats.repository.PipelineStageRepository;

@Service
public class ApplicationService {

    private final ApplicationRepository applications;
    private final PipelineStageRepository stages;
    private final ApplicationStageHistoryRepository history;
    private final AuditLogRepository audits;
    private final IdempotencyKeyRepository idempotencyKeys;
    private final InternalServiceClient internalClient;
    private final NotificationPublisher notifications;
    private final ObjectMapper objectMapper;

    public ApplicationService(
            ApplicationRepository applications,
            PipelineStageRepository stages,
            ApplicationStageHistoryRepository history,
            AuditLogRepository audits,
            IdempotencyKeyRepository idempotencyKeys,
            InternalServiceClient internalClient,
            NotificationPublisher notifications,
            ObjectMapper objectMapper) {
        this.applications = applications;
        this.stages = stages;
        this.history = history;
        this.audits = audits;
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
        if (cv == null || !candidateId.equals(cv.candidateId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ERR_CV_NOT_OWNED",
                    "The selected CV does not belong to the candidate");
        }
        if (applications.existsByJobIdAndCandidateId(request.jobId(), candidateId)) {
            throw new ApiException(HttpStatus.CONFLICT, "ERR_DUPLICATE_APPLICATION",
                    "Candidate has already applied to this job");
        }

        PipelineStage firstStage = ensureDefaultStages(tenantId, request.jobId());
        Application application = new Application();
        application.setTenantId(tenantId);
        application.setJobId(request.jobId());
        application.setCandidateId(candidateId);
        application.setCvId(request.cvId());
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
        notifications.publish(new NotificationEvent(tenantId, job.recruiterId(),
                "APPLICATION_SUBMITTED", "APPLICATION", application.getId(),
                "New application", "A candidate submitted an application"));
        return ApplicationResponse.from(application);
    }

    @Transactional(readOnly = true)
    public ApplicationResponse get(UUID id) {
        return ApplicationResponse.from(requireReadableApplication(id));
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
                    return StageColumn.from(stage, page.getContent(), page.getTotalElements(), page.hasNext());
                })
                .toList();
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
        return pageResponse(result);
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
        writeAudit(application, before, ipAddress, deviceInfo);

        notifications.publish(new NotificationEvent(application.getTenantId(), application.getCandidateId(),
                "APPLICATION_STAGE_CHANGED", "APPLICATION", application.getId(),
                "Application updated", "Your application moved to " + target.getName()));
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
        writeAudit(application, before, ipAddress, deviceInfo);

        var job = internalClient.getJobStatus(application.getJobId());
        notifications.publish(new NotificationEvent(application.getTenantId(), job.recruiterId(),
                "APPLICATION_WITHDRAWN", "APPLICATION", application.getId(),
                "Application withdrawn", "A candidate withdrew an application"));
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
                        candidateView ? null : item.getReason(),
                        item.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getInternal(UUID id) {
        return ApplicationResponse.from(requireEmployerApplication(id));
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
            throw new ApiException(HttpStatus.FORBIDDEN, "ERR_TENANT_MISMATCH",
                    "Application belongs to another tenant");
        }
        return application;
    }

    private Application requireEmployerApplication(UUID id) {
        UUID tenantId = requireEmployerTenant();
        return applications.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ERR_APPLICATION_NOT_FOUND",
                        "Application was not found"));
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
        if (java.util.Arrays.stream(allowedRoles).noneMatch(RequestContext.role()::equals)) {
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
        return new PageResponse<>(page.getContent().stream().map(ApplicationResponse::from).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
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
        item.setReason(reason);
        history.save(item);
    }

    private void writeAudit(
            Application application,
            Map<String, Object> before,
            String ipAddress,
            String deviceInfo) {
        AuditLog audit = new AuditLog();
        audit.setTenantId(application.getTenantId());
        audit.setActorId(RequestContext.userId());
        audit.setActionType("APPLICATION_STAGE_CHANGED");
        audit.setEntityType("APPLICATION");
        audit.setEntityId(application.getId());
        audit.setBeforeState(objectMapper.valueToTree(before));
        audit.setAfterState(objectMapper.valueToTree(Map.of(
                "stageId", application.getCurrentStage().getId(),
                "stageName", application.getCurrentStage().getName(),
                "status", application.getStatus())));
        audit.setIpAddress(ipAddress);
        audit.setDeviceInfo(deviceInfo);
        audits.save(audit);
    }
}
