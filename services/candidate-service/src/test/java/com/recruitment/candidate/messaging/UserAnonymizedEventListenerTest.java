package com.recruitment.candidate.messaging;

import com.recruitment.candidate.service.CandidateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class UserAnonymizedEventListenerTest {

    @Mock
    private CandidateService candidateService;

    @InjectMocks
    private UserAnonymizedEventListener listener;

    @Test
    void onUserAnonymizedInvokesCandidateService() {
        UUID userId = UUID.randomUUID();
        UserAnonymizedEventListener.UserAnonymizedEvent event =
                new UserAnonymizedEventListener.UserAnonymizedEvent("USER_ANONYMIZED", userId, "anonymized@deleted.local", Instant.now());

        listener.onUserAnonymized(event);

        verify(candidateService).anonymizeCandidate(userId);
    }

    @Test
    void onUserAnonymizedIgnoresNull() {
        listener.onUserAnonymized(null);
        listener.onUserAnonymized(new UserAnonymizedEventListener.UserAnonymizedEvent("USER_ANONYMIZED", null, null, null));

        verify(candidateService, never()).anonymizeCandidate(org.mockito.ArgumentMatchers.any());
    }
}
