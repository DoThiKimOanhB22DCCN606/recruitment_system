package com.rms.offer.messaging;

import com.rms.offer.client.AtsServiceClient;
import com.rms.offer.entity.Offer;
import com.rms.offer.messaging.event.OfferAcceptanceRetryEvent;
import com.rms.offer.repository.OfferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfferAcceptanceRetryConsumerTest {

    @Mock
    private AtsServiceClient atsServiceClient;

    @Mock
    private OfferRepository offerRepository;

    private OfferAcceptanceRetryConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new OfferAcceptanceRetryConsumer(atsServiceClient, offerRepository);
    }

    @Test
    void processesRetrySuccessfully() {
        UUID offerId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID hrAdminId = UUID.randomUUID();

        Offer offer = new Offer();
        offer.setId(offerId);
        offer.setTenantId(tenantId);
        offer.setCreatedBy(hrAdminId);

        when(offerRepository.findByIdAndTenantId(offerId, tenantId)).thenReturn(Optional.of(offer));

        OfferAcceptanceRetryEvent event = new OfferAcceptanceRetryEvent(offerId, applicationId, tenantId);
        consumer.onOfferAcceptanceRetry(event);

        verify(atsServiceClient).hireApplication(applicationId, tenantId, hrAdminId);
    }
}
