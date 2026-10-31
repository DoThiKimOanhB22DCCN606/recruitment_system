package com.rms.offer.service;

import com.rms.common.exception.ErrorCode;
import com.rms.common.exception.RmsException;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Mock
    private com.rms.offer.repository.AuditLogRepository auditLogRepository;

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
        validRequest.setStartDate(java.time.LocalDate.now().plusDays(30));
        validRequest.setExpiryDate(java.time.LocalDate.now().plusDays(7));
    }

    @Test
    void createDraftOffer_Success() {
        when(atsServiceClient.validateApplication(applicationId, tenantId)).thenReturn(true);
        when(offerRepository.findAllByApplicationIdAndTenantId(applicationId, tenantId)).thenReturn(Collections.emptyList());
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
        verify(auditLogRepository).save(any());
        verify(offerEventPublisher).publishOfferStatusChangedEvent(eq(offerId), eq(applicationId), eq(tenantId), isNull(), eq("DRAFT"));
    }

    @Test
    void createDraftOffer_InvalidApplicationStageThrowsRmsException() {
        when(atsServiceClient.validateApplication(applicationId, tenantId)).thenReturn(false);

        assertThatThrownBy(() -> offerService.createDraftOffer(validRequest, tenantId, userId))
                .isInstanceOf(RmsException.class)
                .satisfies(e -> assertThat(((RmsException) e).getErrorCode()).isEqualTo(ErrorCode.ERR_OFFER_INVALID_STAGE));

        verify(offerRepository, never()).save(any());
    }

    @Test
    void createDraftOffer_DuplicateActiveOfferThrowsRmsException() {
        when(atsServiceClient.validateApplication(applicationId, tenantId)).thenReturn(true);
        Offer existing = new Offer();
        existing.setStatus(OfferStatus.SENT);
        when(offerRepository.findAllByApplicationIdAndTenantId(applicationId, tenantId)).thenReturn(List.of(existing));

        assertThatThrownBy(() -> offerService.createDraftOffer(validRequest, tenantId, userId))
                .isInstanceOf(RmsException.class)
                .satisfies(e -> assertThat(((RmsException) e).getErrorCode()).isEqualTo(ErrorCode.ERR_OFFER_ALREADY_EXISTS));

        verify(offerRepository, never()).save(any());
    }
    
    @Test
    void createDraftOffer_ExpiryDateAfterStartDateThrowsRmsException() {
        when(atsServiceClient.validateApplication(applicationId, tenantId)).thenReturn(true);
        when(offerRepository.findAllByApplicationIdAndTenantId(applicationId, tenantId)).thenReturn(Collections.emptyList());
        
        validRequest.setExpiryDate(java.time.LocalDate.now().plusDays(40)); // After start date (30)

        assertThatThrownBy(() -> offerService.createDraftOffer(validRequest, tenantId, userId))
                .isInstanceOf(RmsException.class)
                .satisfies(e -> assertThat(((RmsException) e).getErrorCode()).isEqualTo(ErrorCode.ERR_VALIDATION_FAILED));

        verify(offerRepository, never()).save(any());
    }

    @Test
    void createDraftOffer_PastExpiryDateThrowsRmsException() {
        when(atsServiceClient.validateApplication(applicationId, tenantId)).thenReturn(true);
        when(offerRepository.findAllByApplicationIdAndTenantId(applicationId, tenantId)).thenReturn(Collections.emptyList());
        
        validRequest.setExpiryDate(java.time.LocalDate.now().minusDays(2)); // In past

        assertThatThrownBy(() -> offerService.createDraftOffer(validRequest, tenantId, userId))
                .isInstanceOf(RmsException.class)
                .satisfies(e -> assertThat(((RmsException) e).getErrorCode()).isEqualTo(ErrorCode.ERR_VALIDATION_FAILED));

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
        verify(auditLogRepository).save(any());
        verify(offerEventPublisher).publishOfferStatusChangedEvent(eq(offerId), eq(applicationId), eq(tenantId), eq("DRAFT"), eq("SENT"));
    }

    @Test
    void acceptOffer_Success() {
        Offer offer = new Offer();
        offer.setId(offerId);
        offer.setCandidateId(candidateId);
        offer.setStatus(OfferStatus.SENT);
        offer.setApplicationId(applicationId);
        offer.setCreatedBy(userId);

        when(offerRepository.findByIdAndTenantId(offerId, tenantId)).thenReturn(Optional.of(offer));
        doNothing().when(atsServiceClient).hireApplication(applicationId, tenantId, userId);

        boolean result = offerService.acceptOffer(offerId, tenantId, candidateId);

        assertTrue(result);
        assertEquals(OfferStatus.ACCEPTED, offer.getStatus());
        assertNotNull(offer.getRespondedAt());
        verify(atsServiceClient).hireApplication(applicationId, tenantId, userId);
        verify(auditLogRepository).save(any());
        verify(offerEventPublisher, never()).publishOfferAcceptanceRetryEvent(any(), any(), any());
    }
    
    @Test
    void acceptOffer_AtsFailure_TriggersRetry() {
        Offer offer = new Offer();
        offer.setId(offerId);
        offer.setCandidateId(candidateId);
        offer.setStatus(OfferStatus.SENT);
        offer.setApplicationId(applicationId);
        offer.setCreatedBy(userId);

        when(offerRepository.findByIdAndTenantId(offerId, tenantId)).thenReturn(Optional.of(offer));
        doThrow(new RuntimeException("ATS unavailable")).when(atsServiceClient).hireApplication(applicationId, tenantId, userId);

        boolean result = offerService.acceptOffer(offerId, tenantId, candidateId);

        assertFalse(result);
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
