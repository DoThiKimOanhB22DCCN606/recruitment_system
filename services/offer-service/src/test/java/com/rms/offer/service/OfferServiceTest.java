package com.rms.offer.service;

import com.rms.offer.client.AtsServiceClient;
import com.rms.offer.dto.OfferRequest;
import com.rms.offer.entity.Offer;
import com.rms.offer.entity.OfferStatus;
import com.rms.offer.exception.OfferException;
import com.rms.offer.messaging.OfferEventPublisher;
import com.rms.offer.repository.OfferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfferServiceTest {

    @Mock
    private OfferRepository offerRepository;

    @Mock
    private AtsServiceClient atsServiceClient;

    @Mock
    private OfferEventPublisher offerEventPublisher;

    @InjectMocks
    private OfferService offerService;

    private UUID tenantId;
    private UUID userId;
    private UUID candidateId;
    private UUID applicationId;
    private UUID offerId;
    private OfferRequest validRequest;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        candidateId = UUID.randomUUID();
        applicationId = UUID.randomUUID();
        offerId = UUID.randomUUID();

        validRequest = new OfferRequest();
        validRequest.setApplicationId(applicationId);
        validRequest.setCandidateId(candidateId);
        validRequest.setProposedSalary(BigDecimal.valueOf(100000));
        validRequest.setCurrency("USD");
        validRequest.setStartDate(LocalDate.now().plusDays(30));
        validRequest.setExpiryDate(LocalDate.now().plusDays(7));
    }

    @Test
    void createDraftOffer_Success() {
        when(atsServiceClient.validateApplication(applicationId, tenantId)).thenReturn(true);
        when(offerRepository.save(any(Offer.class))).thenAnswer(i -> {
            Offer o = i.getArgument(0);
            o.setId(offerId);
            return o;
        });

        Offer offer = offerService.createDraftOffer(validRequest, tenantId, userId);

        assertNotNull(offer);
        assertEquals(OfferStatus.DRAFT, offer.getStatus());
        assertEquals(offerId, offer.getId());
        verify(atsServiceClient).validateApplication(applicationId, tenantId);
        verify(offerRepository).save(any(Offer.class));
    }

    @Test
    void createDraftOffer_InvalidApplication() {
        when(atsServiceClient.validateApplication(applicationId, tenantId)).thenReturn(false);

        OfferException exception = assertThrows(OfferException.class, () -> {
            offerService.createDraftOffer(validRequest, tenantId, userId);
        });
        assertTrue(exception.getMessage().contains("Invalid application"));
        verify(offerRepository, never()).save(any());
    }
    
    @Test
    void createDraftOffer_InvalidExpiryDate() {
        when(atsServiceClient.validateApplication(applicationId, tenantId)).thenReturn(true);
        
        validRequest.setExpiryDate(LocalDate.now().plusDays(40)); // After start date (30)

        OfferException exception = assertThrows(OfferException.class, () -> {
            offerService.createDraftOffer(validRequest, tenantId, userId);
        });
        assertTrue(exception.getMessage().contains("Expiry date must be before the start date"));
        verify(offerRepository, never()).save(any());
    }

    @Test
    void sendOffer_Success() {
        Offer offer = new Offer();
        offer.setId(offerId);
        offer.setStatus(OfferStatus.DRAFT);
        offer.setApplicationId(applicationId);

        when(offerRepository.findByIdAndTenantId(offerId, tenantId)).thenReturn(Optional.of(offer));

        offerService.sendOffer(offerId, tenantId);

        assertEquals(OfferStatus.SENT, offer.getStatus());
        verify(offerRepository).save(offer);
        verify(offerEventPublisher).publishOfferStatusChangedEvent(eq(offerId), eq(applicationId), eq(tenantId), eq("DRAFT"), eq("SENT"));
    }

    @Test
    void acceptOffer_Success() {
        Offer offer = new Offer();
        offer.setId(offerId);
        offer.setCandidateId(candidateId);
        offer.setStatus(OfferStatus.SENT);
        offer.setApplicationId(applicationId);

        when(offerRepository.findByIdAndTenantId(offerId, tenantId)).thenReturn(Optional.of(offer));
        doNothing().when(atsServiceClient).hireApplication(applicationId, tenantId);

        boolean result = offerService.acceptOffer(offerId, tenantId, candidateId);

        assertTrue(result);
        assertEquals(OfferStatus.ACCEPTED, offer.getStatus());
        assertNotNull(offer.getRespondedAt());
        verify(atsServiceClient).hireApplication(applicationId, tenantId);
        verify(offerEventPublisher, never()).publishOfferAcceptanceRetryEvent(any(), any(), any());
    }
    
    @Test
    void acceptOffer_AtsFailure_TriggersRetry() {
        Offer offer = new Offer();
        offer.setId(offerId);
        offer.setCandidateId(candidateId);
        offer.setStatus(OfferStatus.SENT);
        offer.setApplicationId(applicationId);

        when(offerRepository.findByIdAndTenantId(offerId, tenantId)).thenReturn(Optional.of(offer));
        doThrow(new RuntimeException("ATS unavailable")).when(atsServiceClient).hireApplication(applicationId, tenantId);

        boolean result = offerService.acceptOffer(offerId, tenantId, candidateId);

        assertFalse(result); // Return false indicating retry
        assertEquals(OfferStatus.ACCEPTED, offer.getStatus());
        verify(offerEventPublisher).publishOfferAcceptanceRetryEvent(offerId, applicationId, tenantId);
    }
    
    @Test
    void declineOffer_InvalidState() {
        Offer offer = new Offer();
        offer.setId(offerId);
        offer.setCandidateId(candidateId);
        offer.setStatus(OfferStatus.DRAFT); // Only SENT can be declined

        when(offerRepository.findByIdAndTenantId(offerId, tenantId)).thenReturn(Optional.of(offer));

        assertThrows(OfferException.class, () -> {
            offerService.declineOffer(offerId, tenantId, candidateId);
        });
    }
}
