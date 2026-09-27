package com.recruitment.ats.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitment.ats.api.ApiExceptionHandler.ApiException;
import com.recruitment.ats.api.ApplicationDtos.SubmitApplicationRequest;
import com.recruitment.ats.config.RequestContext;
import com.recruitment.ats.domain.Application;
import com.recruitment.ats.domain.PipelineStage;
import com.recruitment.ats.integration.InternalServiceClient;
import com.recruitment.ats.messaging.NotificationPublisher;
import com.recruitment.ats.repository.ApplicationRepository;
import com.recruitment.ats.repository.ApplicationStageHistoryRepository;
import com.recruitment.ats.repository.AuditLogRepository;
import com.recruitment.ats.repository.IdempotencyKeyRepository;
import com.recruitment.ats.repository.PipelineStageRepository;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    private static final UUID CANDIDATE_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID TENANT_ID = UUID.fromString("20000000-0000-4000-8000-000000000001");
    private static final UUID JOB_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final UUID CV_ID = UUID.fromString("40000000-0000-4000-8000-000000000001");
    private static final String IDEMPOTENCY_KEY = "50000000-0000-4000-8000-000000000001";

    @Mock private ApplicationRepository applications;
    @Mock private PipelineStageRepository stages;
    @Mock private ApplicationStageHistoryRepository history;
    @Mock private AuditService auditService;
    @Mock private IdempotencyKeyRepository idempotencyKeys;
    @Mock private InternalServiceClient internalClient;
    @Mock private NotificationPublisher notifications;

    private ApplicationService service;

    @BeforeEach
    void setUp() {
        service = new ApplicationService(applications, stages, history, auditService, idempotencyKeys,
                internalClient, notifications, new ObjectMapper());
    }

    @AfterEach
    void clearContext() {
        RequestContext.clear();
    }

    @Test
    void candidateWithoutTenantGetsApplicationTenantFromJob() {
        RequestContext.set(CANDIDATE_ID, null, "CANDIDATE");
        PipelineStage firstStage = new PipelineStage(TENANT_ID, JOB_ID, "New", 1, false, null);
        firstStage.setId(UUID.randomUUID());

        when(idempotencyKeys.findById(CANDIDATE_ID + ":SUBMIT_APPLICATION:" + IDEMPOTENCY_KEY))
                .thenReturn(Optional.empty());
        when(internalClient.getJobStatus(JOB_ID)).thenReturn(new InternalServiceClient.JobStatus(
                JOB_ID, TENANT_ID, "PUBLISHED", UUID.randomUUID()));
        when(internalClient.getCv(CV_ID, CANDIDATE_ID))
                .thenReturn(new InternalServiceClient.CvDetails(CV_ID, CANDIDATE_ID));
        when(stages.findFirstByJobIdAndTenantIdOrderBySequenceOrder(JOB_ID, TENANT_ID))
                .thenReturn(Optional.of(firstStage));
        when(applications.saveAndFlush(any(Application.class))).thenAnswer(invocation -> {
            Application application = invocation.getArgument(0);
            application.setId(UUID.randomUUID());
            return application;
        });

        var response = service.submit(new SubmitApplicationRequest(JOB_ID, CV_ID, null), IDEMPOTENCY_KEY);

        assertThat(response.tenantId()).isEqualTo(TENANT_ID);
        assertThat(response.candidateId()).isEqualTo(CANDIDATE_ID);
        verify(idempotencyKeys).findById(CANDIDATE_ID + ":SUBMIT_APPLICATION:" + IDEMPOTENCY_KEY);
    }

    @Test
    void candidateCannotOpenEmployerKanban() {
        RequestContext.set(CANDIDATE_ID, null, "CANDIDATE");

        assertThatThrownBy(() -> service.kanban(JOB_ID))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("permission");
    }

    @Test
    void hireInternalTransitionsStatusToHiredAndPublishesNotification() {
        UUID applicationId = UUID.randomUUID();
        UUID recruiterId = UUID.randomUUID();
        Application application = new Application();
        application.setId(applicationId);
        application.setTenantId(TENANT_ID);
        application.setJobId(JOB_ID);
        application.setCandidateId(CANDIDATE_ID);
        application.setStatus(com.recruitment.ats.domain.ApplicationStatus.ACTIVE);

        PipelineStage hiredStage = new PipelineStage(TENANT_ID, JOB_ID, "Hired", 5, true, com.recruitment.ats.domain.ApplicationStatus.HIRED);
        hiredStage.setId(UUID.randomUUID());

        when(applications.findByIdAndTenantId(applicationId, TENANT_ID)).thenReturn(Optional.of(application));
        when(stages.findFirstByJobIdAndTenantIdAndTerminalStatus(JOB_ID, TENANT_ID, com.recruitment.ats.domain.ApplicationStatus.HIRED))
                .thenReturn(Optional.of(hiredStage));
        when(internalClient.getJobStatus(JOB_ID)).thenReturn(new InternalServiceClient.JobStatus(JOB_ID, TENANT_ID, "PUBLISHED", recruiterId));

        var response = service.hireInternal(applicationId, TENANT_ID);

        assertThat(response.status()).isEqualTo(com.recruitment.ats.domain.ApplicationStatus.HIRED);
        assertThat(application.getStatus()).isEqualTo(com.recruitment.ats.domain.ApplicationStatus.HIRED);
        verify(history).save(any());
        verify(auditService).save(any());
        verify(notifications).publish(any());
    }

    @Test
    void hireInternalWithExplicitActorIdRecordsAdminInAuditLog() {
        UUID applicationId = UUID.randomUUID();
        UUID recruiterId = UUID.randomUUID();
        UUID hrAdminId = UUID.randomUUID();
        Application application = new Application();
        application.setId(applicationId);
        application.setTenantId(TENANT_ID);
        application.setJobId(JOB_ID);
        application.setCandidateId(CANDIDATE_ID);
        application.setStatus(com.recruitment.ats.domain.ApplicationStatus.ACTIVE);

        PipelineStage hiredStage = new PipelineStage(TENANT_ID, JOB_ID, "Hired", 5, true, com.recruitment.ats.domain.ApplicationStatus.HIRED);
        hiredStage.setId(UUID.randomUUID());

        when(applications.findByIdAndTenantId(applicationId, TENANT_ID)).thenReturn(Optional.of(application));
        when(stages.findFirstByJobIdAndTenantIdAndTerminalStatus(JOB_ID, TENANT_ID, com.recruitment.ats.domain.ApplicationStatus.HIRED))
                .thenReturn(Optional.of(hiredStage));
        when(internalClient.getJobStatus(JOB_ID)).thenReturn(new InternalServiceClient.JobStatus(JOB_ID, TENANT_ID, "PUBLISHED", recruiterId));

        var response = service.hireInternal(applicationId, TENANT_ID, hrAdminId);

        assertThat(response.status()).isEqualTo(com.recruitment.ats.domain.ApplicationStatus.HIRED);
        org.mockito.ArgumentCaptor<com.recruitment.ats.domain.AuditLog> auditCaptor =
                org.mockito.ArgumentCaptor.forClass(com.recruitment.ats.domain.AuditLog.class);
        verify(auditService).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getActorId()).isEqualTo(hrAdminId);
    }

    @Test
    void crossTenantAccessLogsAuditRecordAndReturns403() {
        UUID applicationId = UUID.randomUUID();
        UUID otherTenantId = UUID.fromString("99999999-9999-4000-8000-000000000099");
        UUID recruiterId = UUID.randomUUID();

        RequestContext.set(recruiterId, otherTenantId, "RECRUITER");

        Application application = new Application();
        application.setId(applicationId);
        application.setTenantId(TENANT_ID); // Different tenant

        when(applications.findOneById(applicationId)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> service.get(applicationId))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> {
                    ApiException ae = (ApiException) e;
                    assertThat(ae.status()).isEqualTo(org.springframework.http.HttpStatus.FORBIDDEN);
                    assertThat(ae.code()).isEqualTo("ERR_TENANT_MISMATCH");
                });

        verify(auditService).logCrossTenantAttempt(otherTenantId, recruiterId, applicationId);
    }

    @Test
    void submitBroadcastsNotificationsToAllTenantRecruiters() {
        RequestContext.set(CANDIDATE_ID, null, "CANDIDATE", "Candidate Jane");
        PipelineStage firstStage = new PipelineStage(TENANT_ID, JOB_ID, "New", 1, false, null);
        firstStage.setId(UUID.randomUUID());

        UUID recruiter1 = UUID.randomUUID();
        UUID recruiter2 = UUID.randomUUID();

        when(idempotencyKeys.findById(CANDIDATE_ID + ":SUBMIT_APPLICATION:" + IDEMPOTENCY_KEY))
                .thenReturn(Optional.empty());
        when(internalClient.getJobStatus(JOB_ID)).thenReturn(new InternalServiceClient.JobStatus(
                JOB_ID, TENANT_ID, "PUBLISHED", recruiter1));
        when(internalClient.getCv(CV_ID, CANDIDATE_ID))
                .thenReturn(new InternalServiceClient.CvDetails(CV_ID, CANDIDATE_ID));
        when(internalClient.getTenantRecruiterIds(TENANT_ID))
                .thenReturn(java.util.List.of(recruiter1, recruiter2));
        when(stages.findFirstByJobIdAndTenantIdOrderBySequenceOrder(JOB_ID, TENANT_ID))
                .thenReturn(Optional.of(firstStage));
        when(applications.saveAndFlush(any(Application.class))).thenAnswer(invocation -> {
            Application application = invocation.getArgument(0);
            application.setId(UUID.randomUUID());
            return application;
        });

        service.submit(new SubmitApplicationRequest(JOB_ID, CV_ID, null), IDEMPOTENCY_KEY);

        verify(notifications, org.mockito.Mockito.times(2)).publish(any());
    }

    @Test
    void stageHistoryIncludesDenormalizedActorName() {
        UUID applicationId = UUID.randomUUID();
        UUID stage1 = UUID.randomUUID();
        UUID stage2 = UUID.randomUUID();
        UUID recruiterId = UUID.randomUUID();

        RequestContext.set(recruiterId, TENANT_ID, "RECRUITER", "Alice Recruiter");

        Application application = new Application();
        application.setId(applicationId);
        application.setTenantId(TENANT_ID);

        com.recruitment.ats.domain.ApplicationStageHistory hist = new com.recruitment.ats.domain.ApplicationStageHistory();
        hist.setId(UUID.randomUUID());
        hist.setApplication(application);
        hist.setFromStageId(stage1);
        hist.setToStageId(stage2);
        hist.setActorId(recruiterId);
        hist.setActorName("Alice Recruiter");
        hist.setReason("Moved to screening");

        PipelineStage p1 = new PipelineStage(TENANT_ID, JOB_ID, "New", 1, false, null);
        p1.setId(stage1);
        PipelineStage p2 = new PipelineStage(TENANT_ID, JOB_ID, "Screening", 2, false, null);
        p2.setId(stage2);

        when(applications.findOneById(applicationId)).thenReturn(Optional.of(application));
        when(history.findByApplicationIdOrderByCreatedAtAsc(applicationId)).thenReturn(java.util.List.of(hist));
        when(stages.findAllById(any())).thenReturn(java.util.List.of(p1, p2));

        var result = service.stageHistory(applicationId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).actorName()).isEqualTo("Alice Recruiter");
    }

    @Test
    void validateInternalReturnsTrueOnlyWhenActiveAndOfferedStage() {
        UUID appId = UUID.randomUUID();
        Application app = new Application();
        app.setId(appId);
        app.setTenantId(TENANT_ID);
        app.setStatus(com.recruitment.ats.domain.ApplicationStatus.ACTIVE);
        PipelineStage offeredStage = new PipelineStage(TENANT_ID, JOB_ID, "Offered", 4, false, null);
        app.setCurrentStage(offeredStage);

        when(applications.findByIdAndTenantId(appId, TENANT_ID)).thenReturn(Optional.of(app));

        boolean valid = service.validateInternal(appId, TENANT_ID);
        assertThat(valid).isTrue();
    }

    @Test
    void validateInternalReturnsFalseWhenInScreeningStage() {
        UUID appId = UUID.randomUUID();
        Application app = new Application();
        app.setId(appId);
        app.setTenantId(TENANT_ID);
        app.setStatus(com.recruitment.ats.domain.ApplicationStatus.ACTIVE);
        PipelineStage screeningStage = new PipelineStage(TENANT_ID, JOB_ID, "Screening", 2, false, null);
        app.setCurrentStage(screeningStage);

        when(applications.findByIdAndTenantId(appId, TENANT_ID)).thenReturn(Optional.of(app));

        boolean valid = service.validateInternal(appId, TENANT_ID);
        assertThat(valid).isFalse();
    }

    @Test
    void testWithdrawPersistsReasonInStageHistoryAndAudit() {
        UUID appId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        RequestContext.set(candidateId, TENANT_ID, "CANDIDATE", "Jane Candidate");

        Application app = new Application();
        app.setId(appId);
        app.setTenantId(TENANT_ID);
        app.setCandidateId(candidateId);
        app.setJobId(JOB_ID);
        app.setStatus(com.recruitment.ats.domain.ApplicationStatus.ACTIVE);
        PipelineStage stage = new PipelineStage(TENANT_ID, JOB_ID, "Screening", 2, false, null);
        stage.setId(UUID.randomUUID());
        app.setCurrentStage(stage);

        when(applications.findOneById(appId)).thenReturn(Optional.of(app));
        when(internalClient.getJobStatus(JOB_ID))
                .thenReturn(new com.recruitment.ats.integration.InternalServiceClient.JobStatus(JOB_ID, TENANT_ID, "PUBLISHED", UUID.randomUUID()));

        service.withdraw(appId, "Accepted another offer", "127.0.0.1", "Chrome");

        assertThat(app.getStatus()).isEqualTo(com.recruitment.ats.domain.ApplicationStatus.WITHDRAWN);
        verify(history).save(argThat(h -> "Accepted another offer".equals(h.getReason()) && candidateId.equals(h.getActorId())));
        verify(auditService).save(argThat(a -> "APPLICATION_WITHDRAWN".equals(a.getActionType()) && candidateId.equals(a.getActorId())));
    }

    @Test
    void testHandleOfferExpiredTransitionsToRejectedAndEmitsNotifications() {
        UUID appId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();
        UUID recruiterId = UUID.randomUUID();

        Application app = new Application();
        app.setId(appId);
        app.setTenantId(TENANT_ID);
        app.setJobId(JOB_ID);
        app.setCandidateId(CANDIDATE_ID);
        app.setStatus(com.recruitment.ats.domain.ApplicationStatus.ACTIVE);
        PipelineStage offeredStage = new PipelineStage(TENANT_ID, JOB_ID, "Offered", 4, false, null);
        offeredStage.setId(UUID.randomUUID());
        app.setCurrentStage(offeredStage);

        PipelineStage rejectedStage = new PipelineStage(TENANT_ID, JOB_ID, "Rejected", 6, true, com.recruitment.ats.domain.ApplicationStatus.REJECTED);
        rejectedStage.setId(UUID.randomUUID());

        when(applications.findByIdAndTenantId(appId, TENANT_ID)).thenReturn(Optional.of(app));
        when(stages.findFirstByJobIdAndTenantIdAndTerminalStatus(JOB_ID, TENANT_ID, com.recruitment.ats.domain.ApplicationStatus.REJECTED))
                .thenReturn(Optional.of(rejectedStage));
        when(internalClient.getTenantRecruiterIds(TENANT_ID)).thenReturn(java.util.List.of(recruiterId));
        when(internalClient.getJobStatus(JOB_ID)).thenReturn(new com.recruitment.ats.integration.InternalServiceClient.JobStatus(JOB_ID, TENANT_ID, "PUBLISHED", recruiterId));

        service.handleOfferExpired(offerId, appId, TENANT_ID);

        assertThat(app.getStatus()).isEqualTo(com.recruitment.ats.domain.ApplicationStatus.REJECTED);
        assertThat(app.getRejectionReason()).isEqualTo("Offer expired");
        assertThat(app.getCurrentStage()).isEqualTo(rejectedStage);

        verify(applications).save(app);
        verify(history).save(argThat(h -> "Offer expired".equals(h.getReason()) && rejectedStage.getId().equals(h.getToStageId())));
        verify(auditService).save(argThat(a -> "OFFER_EXPIRED".equals(a.getActionType())));
        verify(notifications).publish(argThat(n -> "OFFER_EXPIRED".equals(n.actionType()) && recruiterId.equals(n.recipientUserId())));
    }

    @Test
    void testHandleOfferExpiredIgnoresAlreadyTerminalApplication() {
        UUID appId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();

        Application app = new Application();
        app.setId(appId);
        app.setTenantId(TENANT_ID);
        app.setStatus(com.recruitment.ats.domain.ApplicationStatus.HIRED);

        when(applications.findByIdAndTenantId(appId, TENANT_ID)).thenReturn(Optional.of(app));

        service.handleOfferExpired(offerId, appId, TENANT_ID);

        assertThat(app.getStatus()).isEqualTo(com.recruitment.ats.domain.ApplicationStatus.HIRED);
        verify(applications, org.mockito.Mockito.never()).save(app);
    }
}
