package com.rms.offer.service;

import com.rms.offer.client.AtsServiceClient;
import com.rms.offer.dto.OfferRequest;
import com.rms.offer.entity.Offer;
import com.rms.offer.entity.OfferStatus;
import com.rms.offer.exception.OfferException;
import com.rms.offer.repository.OfferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class OfferService {

    private final OfferRepository offerRepository;
    private final AtsServiceClient atsServiceClient;
    private final com.rms.offer.messaging.OfferEventPublisher offerEventPublisher;

    public OfferService(OfferRepository offerRepository, AtsServiceClient atsServiceClient, com.rms.offer.messaging.OfferEventPublisher offerEventPublisher) {
        this.offerRepository = offerRepository;
        this.atsServiceClient = atsServiceClient;
        this.offerEventPublisher = offerEventPublisher;
    }

    @Transactional
    public Offer createDraftOffer(OfferRequest request, UUID tenantId, UUID userId) {
        if (!atsServiceClient.validateApplication(request.getApplicationId(), tenantId)) {
            throw new OfferException("Invalid application or candidate mismatch.");
        }
        
        if (request.getExpiryDate() != null && request.getStartDate() != null && !request.getExpiryDate().isBefore(request.getStartDate())) {
            throw new OfferException("Expiry date must be before the start date.");
        }

        Offer offer = new Offer();
        offer.setApplicationId(request.getApplicationId());
        offer.setCandidateId(request.getCandidateId());
        offer.setTenantId(tenantId);
        offer.setProposedSalary(request.getProposedSalary());
        offer.setCurrency(request.getCurrency());
        offer.setStartDate(request.getStartDate());
        offer.setExpiryDate(request.getExpiryDate());
        offer.setNotes(request.getNotes());
        offer.setStatus(OfferStatus.DRAFT);
        offer.setCreatedBy(userId);

        return offerRepository.save(offer);
    }



    @Transactional
    public void sendOffer(UUID offerId, UUID tenantId) {
        Offer offer = getOfferForTenant(offerId, tenantId);
        if (offer.getStatus() != OfferStatus.DRAFT) {
            throw new OfferException("Only DRAFT offers can be sent.");
        }
        String oldStatus = offer.getStatus().name();
        offer.setStatus(OfferStatus.SENT);
        offerRepository.save(offer);
        offerEventPublisher.publishOfferStatusChangedEvent(offerId, offer.getApplicationId(), tenantId, oldStatus, OfferStatus.SENT.name());
    }

    @Transactional
    public boolean acceptOffer(UUID offerId, UUID tenantId, UUID userId) {
        Offer offer = getOfferForTenant(offerId, tenantId);
        if (!offer.getCandidateId().equals(userId)) {
            throw new OfferException("Only the assigned candidate can accept this offer.");
        }
        if (offer.getStatus() != OfferStatus.SENT) {
            throw new OfferException("Only SENT offers can be accepted.");
        }
        String oldStatus = offer.getStatus().name();
        offer.setStatus(OfferStatus.ACCEPTED);
        offer.setRespondedAt(Instant.now());
        offerRepository.save(offer);
        offerEventPublisher.publishOfferStatusChangedEvent(offerId, offer.getApplicationId(), tenantId, oldStatus, OfferStatus.ACCEPTED.name());
        
        try {
            atsServiceClient.hireApplication(offer.getApplicationId(), tenantId);
            return true; // Successfully called ATS
        } catch (Exception e) {
            offerEventPublisher.publishOfferAcceptanceRetryEvent(offerId, offer.getApplicationId(), tenantId);
            return false; // ATS call failed, retry queued
        }
    }

    @Transactional
    public void declineOffer(UUID offerId, UUID tenantId, UUID userId) {
        Offer offer = getOfferForTenant(offerId, tenantId);
        if (!offer.getCandidateId().equals(userId)) {
            throw new OfferException("Only the assigned candidate can decline this offer.");
        }
        if (offer.getStatus() != OfferStatus.SENT) {
            throw new OfferException("Only SENT offers can be declined.");
        }
        String oldStatus = offer.getStatus().name();
        offer.setStatus(OfferStatus.DECLINED);
        offer.setRespondedAt(Instant.now());
        offerRepository.save(offer);
        offerEventPublisher.publishOfferStatusChangedEvent(offerId, offer.getApplicationId(), tenantId, oldStatus, OfferStatus.DECLINED.name());
    }

    @Transactional
    public void withdrawOffer(UUID offerId, UUID tenantId) {
        Offer offer = getOfferForTenant(offerId, tenantId);
        if (offer.getStatus() != OfferStatus.DRAFT && offer.getStatus() != OfferStatus.SENT) {
            throw new OfferException("Only DRAFT or SENT offers can be withdrawn.");
        }
        String oldStatus = offer.getStatus().name();
        offer.setStatus(OfferStatus.WITHDRAWN);
        offerRepository.save(offer);
        offerEventPublisher.publishOfferStatusChangedEvent(offerId, offer.getApplicationId(), tenantId, oldStatus, OfferStatus.WITHDRAWN.name());
    }

    public java.util.List<Offer> getOffers(UUID tenantId, UUID candidateId, UUID applicationId) {
        if (candidateId != null) {
            return offerRepository.findAllByCandidateIdAndTenantId(candidateId, tenantId);
        } else if (applicationId != null) {
            return offerRepository.findAllByApplicationIdAndTenantId(applicationId, tenantId);
        } else {
            // Note: In real application, this should be paginated
            return offerRepository.findAllByTenantId(tenantId, org.springframework.data.domain.Pageable.unpaged()).getContent();
        }
    }

    public Offer getOffer(UUID offerId, UUID tenantId) {
        return getOfferForTenant(offerId, tenantId);
    }

    @Transactional
    public Offer updateOffer(UUID offerId, UUID tenantId, OfferRequest request) {
        Offer offer = getOfferForTenant(offerId, tenantId);
        if (offer.getStatus() != OfferStatus.DRAFT) {
            throw new OfferException("Only DRAFT offers can be updated.");
        }
        
        if (request.getExpiryDate() != null && request.getStartDate() != null && !request.getExpiryDate().isBefore(request.getStartDate())) {
            throw new OfferException("Expiry date must be before the start date.");
        }
        
        offer.setProposedSalary(request.getProposedSalary());
        offer.setCurrency(request.getCurrency());
        offer.setStartDate(request.getStartDate());
        offer.setExpiryDate(request.getExpiryDate());
        offer.setNotes(request.getNotes());
        
        return offerRepository.save(offer);
    }

    private Offer getOfferForTenant(UUID offerId, UUID tenantId) {
        return offerRepository.findByIdAndTenantId(offerId, tenantId)
                .orElseThrow(() -> new OfferException("Offer not found."));
    }
}
