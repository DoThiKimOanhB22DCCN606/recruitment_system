package com.rms.iam.scheduler;

import com.rms.iam.entity.User;
import com.rms.iam.entity.UserStatus;
import com.rms.iam.messaging.IamEventPublisher;
import com.rms.iam.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAnonymizationSchedulerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private IamEventPublisher iamEventPublisher;

    @InjectMocks
    private UserAnonymizationScheduler scheduler;

    @Test
    void anonymizeDeactivatedUsersProcessesOldRecords() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("user@example.com");
        user.setPasswordHash("secret_hash");
        user.setStatus(UserStatus.DEACTIVATED);
        user.setUpdatedAt(Instant.now().minus(35, ChronoUnit.DAYS));

        when(userRepository.findDeactivatedUsersOlderThan(eq(UserStatus.DEACTIVATED), any(Instant.class)))
                .thenReturn(List.of(user));

        int anonymizedCount = scheduler.anonymizeDeactivatedUsers();

        assertThat(anonymizedCount).isEqualTo(1);
        assertThat(user.getEmail()).isEqualTo("anonymized_" + userId + "@deleted.local");
        assertThat(user.getPasswordHash()).contains("DUMMY_ANONYMIZED_HASH");

        verify(userRepository).save(user);
        verify(iamEventPublisher).publishUserAnonymizedEvent(eq(userId), eq("anonymized_" + userId + "@deleted.local"));
    }

    @Test
    void anonymizeDeactivatedUsersNoOpWhenNoRecords() {
        when(userRepository.findDeactivatedUsersOlderThan(eq(UserStatus.DEACTIVATED), any(Instant.class)))
                .thenReturn(List.of());

        int count = scheduler.anonymizeDeactivatedUsers();

        assertThat(count).isEqualTo(0);
        verify(userRepository, never()).save(any());
        verify(iamEventPublisher, never()).publishUserAnonymizedEvent(any(), any());
    }
}
