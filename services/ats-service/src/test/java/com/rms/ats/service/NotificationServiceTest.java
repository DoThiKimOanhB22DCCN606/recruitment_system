package com.rms.ats.service;

import com.rms.ats.client.InternalServiceClient;
import com.rms.ats.config.RequestContext;
import com.rms.ats.entity.Application;
import com.rms.ats.entity.Notification;
import com.rms.ats.repository.ApplicationRepository;
import com.rms.ats.repository.ApplicationStageHistoryRepository;
import com.rms.ats.repository.NotificationRepository;
import com.rms.ats.repository.PipelineStageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {
    @AfterEach
    void clearContext() {
        RequestContext.clear();
    }

    @Test
    void oldEnglishApplicationNotificationShowsCandidateAndJobInVietnameseWithoutChangingStoredRow() {
        var notifications = mock(NotificationRepository.class);
        var applications = mock(ApplicationRepository.class);
        var history = mock(ApplicationStageHistoryRepository.class);
        var stages = mock(PipelineStageRepository.class);
        var client = mock(InternalServiceClient.class);
        var service = new NotificationService(notifications, applications, history, stages, client);

        UUID recipientId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        RequestContext.set(recipientId, UUID.randomUUID(), "RECRUITER");

        var old = new Notification();
        old.setActionType("APPLICATION_SUBMITTED");
        old.setEntityId(applicationId);
        old.setTitle("New application");
        old.setMessage("A candidate submitted an application");
        old.setCreatedAt(Instant.now());

        var application = new Application();
        application.setId(applicationId);
        application.setCandidateId(candidateId);
        application.setCvId(new UUID(1L, 2L));
        application.setJobId(new UUID(0L, 4L));
        when(notifications.findByRecipientUserIdAndCreatedAtAfterOrderByCreatedAtDesc(eq(recipientId), any()))
                .thenReturn(List.of(old));
        when(applications.findOneById(applicationId)).thenReturn(Optional.of(application));
        when(client.getCv(2L, candidateId)).thenReturn(new InternalServiceClient.CvDetails(
                2L, candidateId, "Nguyễn Thị Lan", null, null, null, true, null));
        when(client.getJobStatus(4L)).thenReturn(new InternalServiceClient.JobStatus(
                4L, UUID.randomUUID(), "PUBLISHED", recipientId, "Lập trình viên Java"));

        Notification localized = service.list().get(0);

        assertThat(localized.getMessage()).contains("Ứng viên Nguyễn Thị Lan", "Lập trình viên Java");
        assertThat(old.getMessage()).isEqualTo("A candidate submitted an application");
        verify(notifications).findByRecipientUserIdAndCreatedAtAfterOrderByCreatedAtDesc(eq(recipientId), any());
    }
}
