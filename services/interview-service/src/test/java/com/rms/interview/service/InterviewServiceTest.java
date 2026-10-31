package com.rms.interview.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.rms.interview.exception.ApiExceptionHandler.ApiException;
import com.rms.interview.dto.InterviewDtos.CreateInterviewRequest;
import com.rms.interview.config.RequestContext;
import com.rms.interview.entity.InterviewType;
import com.rms.interview.client.InternalServiceClient;
import com.rms.interview.messaging.NotificationPublisher;
import com.rms.interview.repository.InterviewRepository;

@ExtendWith(MockitoExtension.class)
class InterviewServiceTest {

    @Mock private InterviewRepository repository;
    @Mock private InternalServiceClient internalClient;
    @Mock private NotificationPublisher notifications;
    @InjectMocks private InterviewService service;

    @AfterEach
    void clearContext() {
        RequestContext.clear();
    }

    @Test
    void candidateCannotCreateInterview() {
        RequestContext.set(UUID.randomUUID(), null, "CANDIDATE");
        CreateInterviewRequest request = new CreateInterviewRequest(
                UUID.randomUUID(), LocalDate.now().plusDays(1), LocalTime.NOON, 60,
                InterviewType.VIDEO_CALL, "https://meet.example.test", null,
                Set.of(UUID.randomUUID()));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Recruiter or HR Admin");
    }
}
