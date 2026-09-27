package com.recruitment.candidate.messaging;

import com.recruitment.candidate.entity.Candidate;
import com.recruitment.candidate.repository.CandidateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserCreatedEventListenerTest {

    @Mock
    private CandidateRepository candidateRepository;

    private UserCreatedEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new UserCreatedEventListener(candidateRepository);
    }

    @Test
    void createsCandidateWithExactUuid() {
        UUID userId = UUID.randomUUID();
        UserCreatedEventListener.UserCreatedEvent event = new UserCreatedEventListener.UserCreatedEvent(
                "USER_CREATED",
                userId,
                "candidate@test.com",
                "CANDIDATE",
                null,
                "corr-1",
                Instant.now()
        );

        when(candidateRepository.findByUserUuid(userId)).thenReturn(Optional.empty());

        listener.onUserCreated(event);

        ArgumentCaptor<Candidate> captor = ArgumentCaptor.forClass(Candidate.class);
        verify(candidateRepository).save(captor.capture());

        Candidate saved = captor.getValue();
        assertThat(saved.getUserUuid()).isEqualTo(userId);
        assertThat(saved.getEmail()).isEqualTo("candidate@test.com");
    }

    @Test
    void ignoresNonCandidateRoles() {
        UUID userId = UUID.randomUUID();
        UserCreatedEventListener.UserCreatedEvent event = new UserCreatedEventListener.UserCreatedEvent(
                "USER_CREATED",
                userId,
                "recruiter@test.com",
                "RECRUITER",
                null,
                "corr-2",
                Instant.now()
        );

        listener.onUserCreated(event);

        verify(candidateRepository, never()).save(any());
    }
}
