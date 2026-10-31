package com.rms.ats.messaging;

import com.rms.ats.entity.Notification;
import com.rms.ats.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

class NotificationListenerTest {
    @Test
    void repeatedStageChangesEachCreateANotification() {
        NotificationRepository repository = mock(NotificationRepository.class);
        NotificationListener listener = new NotificationListener(repository);
        UUID tenantId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        listener.consume(new NotificationEvent(tenantId, candidateId,
                "APPLICATION_STAGE_CHANGED", "APPLICATION", applicationId,
                "Application updated", "Your application moved to Screening"));
        listener.consume(new NotificationEvent(tenantId, candidateId,
                "APPLICATION_STAGE_CHANGED", "APPLICATION", applicationId,
                "Application updated", "Your application moved to Interviewing"));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository, times(2)).save(captor.capture());
        assertEquals("Your application moved to Screening", captor.getAllValues().get(0).getMessage());
        assertEquals("Your application moved to Interviewing", captor.getAllValues().get(1).getMessage());
    }
}
