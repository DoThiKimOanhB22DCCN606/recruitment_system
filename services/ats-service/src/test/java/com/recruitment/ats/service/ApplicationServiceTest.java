package com.recruitment.ats.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    @Mock private AuditLogRepository audits;
    @Mock private IdempotencyKeyRepository idempotencyKeys;
    @Mock private InternalServiceClient internalClient;
    @Mock private NotificationPublisher notifications;

    private ApplicationService service;

    @BeforeEach
    void setUp() {
        service = new ApplicationService(applications, stages, history, audits, idempotencyKeys,
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
}
