package com.recruitment.ats.messaging;

import com.recruitment.ats.service.ApplicationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class OfferEventListenerTest {

    @Mock
    private ApplicationService applicationService;

    @InjectMocks
    private OfferEventListener listener;

    @Test
    void onOfferExpiredInvokesApplicationService() {
        UUID offerId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        OfferEventListener.OfferExpiredEvent event =
                new OfferEventListener.OfferExpiredEvent(offerId, applicationId, tenantId);

        listener.onOfferExpired(event);

        verify(applicationService).handleOfferExpired(offerId, applicationId, tenantId);
    }

    @Test
    void onOfferExpiredIgnoresNullEventOrNullAppId() {
        listener.onOfferExpired(null);
        listener.onOfferExpired(new OfferEventListener.OfferExpiredEvent(UUID.randomUUID(), null, UUID.randomUUID()));

        verify(applicationService, never()).handleOfferExpired(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
