package com.rms.ats.messaging;

import com.rms.ats.entity.Notification;
import com.rms.ats.repository.NotificationRepository;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CompanyRegistrationNotificationTest {
    @Test
    void notificationIsAddressedToSysAdminWithoutTenant() {
        NotificationRepository repository = mock(NotificationRepository.class);
        UUID registrationId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        new NotificationListener(repository).consumeCompanyRegistration(
                new CompanyRegistrationSubmittedEvent(registrationId, adminId,
                        "Example Co", "hr@example.com"));

        org.mockito.ArgumentCaptor<Notification> captor = org.mockito.ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        Notification notification = captor.getValue();
        assertEquals(adminId, notification.getRecipientUserId());
        assertEquals(registrationId, notification.getEntityId());
        assertNull(notification.getTenantId());
        assertEquals("COMPANY_REGISTRATION", notification.getEntityType());
    }
}
