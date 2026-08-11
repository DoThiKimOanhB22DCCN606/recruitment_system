package com.recruitment.interview.service;

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

import com.recruitment.interview.api.ApiExceptionHandler.ApiException;
import com.recruitment.interview.api.InterviewDtos.CreateInterviewRequest;
import com.recruitment.interview.config.RequestContext;
import com.recruitment.interview.domain.InterviewType;
import com.recruitment.interview.integration.InternalServiceClient;
import com.recruitment.interview.messaging.NotificationPublisher;
import com.recruitment.interview.repository.InterviewRepository;

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
