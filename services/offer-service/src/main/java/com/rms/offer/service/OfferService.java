package com.rms.offer.service;

import com.rms.offer.client.AtsServiceClient;
import com.rms.offer.dto.OfferRequest;
import com.rms.offer.entity.AuditLog;
import com.rms.offer.entity.Offer;
import com.rms.offer.entity.OfferStatus;
import com.rms.offer.exception.OfferException;
import com.rms.offer.repository.AuditLogRepository;
import com.rms.offer.repository.OfferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class OfferService {

    private final OfferRepository offerRepository;
    private final AtsServiceClient atsServiceClient;
    private final com.rms.offer.messaging.OfferEventPublisher offerEventPublisher;
    private final AuditLogRepository auditLogRepository;

    public OfferService(
            OfferRepository offerRepository,
            AtsServiceClient atsServiceClient,
            com.rms.offer.messaging.OfferEventPublisher offerEventPublisher,
            AuditLogRepository auditLogRepository) {
        this.offerRepository = offerRepository;
        this.atsServiceClient = atsServiceClient;
        this.offerEventPublisher = offerEventPublisher;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public Offer createDraftOffer(OfferRequest request, UUID tenantId, UUID userId) {
        if (!atsServiceClient.validateApplication(request.getApplicationId(), tenantId)) {
            throw new com.rms.common.exception.RmsException(
                    com.rms.common.exception.ErrorCode.ERR_OFFER_INVALID_STAGE,
                    "Application is not in Offered stage or does not belong to tenant");
        }

        // Check if active/draft offer already exists for this application
        var existingOffers = offerRepository.findAllByApplicationIdAndTenantId(request.getApplicationId(), tenantId);
        boolean hasActiveOffer = existingOffers.stream().anyMatch(o -> o.getStatus() == OfferStatus.DRAFT || o.getStatus() == OfferStatus.SENT || o.getStatus() == OfferStatus.ACCEPTED);
        if (hasActiveOffer) {
            throw new com.rms.common.exception.RmsException(
                    com.rms.common.exception.ErrorCode.ERR_OFFER_ALREADY_EXISTS,
                    "An active or draft offer already exists for this application");
        }
        
        LocalDate today = LocalDate.now(java.time.ZoneOffset.UTC);
        LocalDate expiryDate = request.getExpiryDate();
        if (expiryDate == null) {
            expiryDate = today.plusDays(7);
        } else if (expiryDate.isBefore(today)) {
            throw new com.rms.common.exception.RmsException(
                    com.rms.common.exception.ErrorCode.ERR_VALIDATION_FAILED,
                    "Expiry date must be in the future");
        }

        if (request.getStartDate() != null && expiryDate.isAfter(request.getStartDate())) {
            throw new com.rms.common.exception.RmsException(
                    com.rms.common.exception.ErrorCode.ERR_VALIDATION_FAILED,
                    "Expiry date must be on or before the start date");
        }

        Offer offer = new Offer();
        offer.setApplicationId(request.getApplicationId());
        offer.setCandidateId(request.getCandidateId());
        offer.setTenantId(tenantId);
        offer.setProposedSalary(request.getProposedSalary());
        offer.setCurrency(request.getCurrency());
        offer.setStartDate(request.getStartDate());
        offer.setExpiryDate(expiryDate);
        offer.setNotes(request.getNotes());
        offer.setStatus(OfferStatus.DRAFT);
        offer.setCreatedBy(userId);

        Offer saved = offerRepository.save(offer);
        logAudit(saved.getId(), tenantId, userId, "CREATE_DRAFT", null, OfferStatus.DRAFT.name(), "Draft offer created");
        offerEventPublisher.publishOfferStatusChangedEvent(saved.getId(), saved.getApplicationId(), tenantId, null, OfferStatus.DRAFT.name());
        return saved;
    }

    @Transactional
    public void sendOffer(UUID offerId, UUID tenantId) {
        sendOffer(offerId, tenantId, null);
    }

    @Transactional
    public void sendOffer(UUID offerId, UUID tenantId, UUID userId) {
        Offer offer = getOfferForTenant(offerId, tenantId);
        if (offer.getStatus() != OfferStatus.DRAFT) {
            throw new OfferException("Only DRAFT offers can be sent.");
        }
        String oldStatus = offer.getStatus().name();
        offer.setStatus(OfferStatus.SENT);
        offerRepository.save(offer);
        UUID actor = userId != null ? userId : offer.getCreatedBy();
        logAudit(offerId, tenantId, actor, "SEND_OFFER", oldStatus, OfferStatus.SENT.name(), "Offer sent to candidate");
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
        logAudit(offerId, tenantId, userId, "ACCEPT_OFFER", oldStatus, OfferStatus.ACCEPTED.name(), "Offer accepted by candidate");
        offerEventPublisher.publishOfferStatusChangedEvent(offerId, offer.getApplicationId(), tenantId, oldStatus, OfferStatus.ACCEPTED.name());
        
        try {
            atsServiceClient.hireApplication(offer.getApplicationId(), tenantId, offer.getCreatedBy());
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
        logAudit(offerId, tenantId, userId, "DECLINE_OFFER", oldStatus, OfferStatus.DECLINED.name(), "Offer declined by candidate");
        offerEventPublisher.publishOfferStatusChangedEvent(offerId, offer.getApplicationId(), tenantId, oldStatus, OfferStatus.DECLINED.name());
    }

    @Transactional
    public void withdrawOffer(UUID offerId, UUID tenantId) {
        withdrawOffer(offerId, tenantId, null);
    }

    @Transactional
    public void withdrawOffer(UUID offerId, UUID tenantId, UUID userId) {
        Offer offer = getOfferForTenant(offerId, tenantId);
        if (offer.getStatus() != OfferStatus.DRAFT && offer.getStatus() != OfferStatus.SENT) {
            throw new OfferException("Only DRAFT or SENT offers can be withdrawn.");
        }
        String oldStatus = offer.getStatus().name();
        offer.setStatus(OfferStatus.WITHDRAWN);
        offerRepository.save(offer);
        UUID actor = userId != null ? userId : offer.getCreatedBy();
        logAudit(offerId, tenantId, actor, "WITHDRAW_OFFER", oldStatus, OfferStatus.WITHDRAWN.name(), "Offer withdrawn by employer");
        offerEventPublisher.publishOfferStatusChangedEvent(offerId, offer.getApplicationId(), tenantId, oldStatus, OfferStatus.WITHDRAWN.name());
    }

    public List<Offer> getOffers(UUID tenantId, UUID candidateId, UUID applicationId) {
        if (candidateId != null) {
            return offerRepository.findAllByCandidateIdAndTenantId(candidateId, tenantId);
        } else if (applicationId != null) {
            return offerRepository.findAllByApplicationIdAndTenantId(applicationId, tenantId);
        } else {
            return offerRepository.findAllByTenantId(tenantId, org.springframework.data.domain.Pageable.unpaged()).getContent();
        }
    }

    public Offer getOffer(UUID offerId, UUID tenantId) {
        return getOfferForTenant(offerId, tenantId);
    }

    public List<AuditLog> getOfferAuditLogs(UUID offerId, UUID tenantId) {
        return auditLogRepository.findByOfferIdAndTenantIdOrderByCreatedAtDesc(offerId, tenantId);
    }

    @Transactional
    public Offer updateOffer(UUID offerId, UUID tenantId, OfferRequest request) {
        return updateOffer(offerId, tenantId, null, request);
    }

    @Transactional
    public Offer updateOffer(UUID offerId, UUID tenantId, UUID userId, OfferRequest request) {
        Offer offer = getOfferForTenant(offerId, tenantId);
        if (offer.getStatus() != OfferStatus.DRAFT) {
            throw new OfferException("Only DRAFT offers can be updated.");
        }
        
        LocalDate today = LocalDate.now(java.time.ZoneOffset.UTC);
        LocalDate expiryDate = request.getExpiryDate();
        if (expiryDate != null) {
            if (expiryDate.isBefore(today)) {
                throw new com.rms.common.exception.RmsException(
                        com.rms.common.exception.ErrorCode.ERR_VALIDATION_FAILED,
                        "Expiry date must be in the future");
            }
            if (request.getStartDate() != null && expiryDate.isAfter(request.getStartDate())) {
                throw new com.rms.common.exception.RmsException(
                        com.rms.common.exception.ErrorCode.ERR_VALIDATION_FAILED,
                        "Expiry date must be on or before the start date");
            }
            offer.setExpiryDate(expiryDate);
        }
        
        offer.setProposedSalary(request.getProposedSalary());
        offer.setCurrency(request.getCurrency());
        offer.setStartDate(request.getStartDate());
        offer.setNotes(request.getNotes());
        
        Offer saved = offerRepository.save(offer);
        UUID actor = userId != null ? userId : offer.getCreatedBy();
        logAudit(offerId, tenantId, actor, "UPDATE_OFFER", offer.getStatus().name(), offer.getStatus().name(), "Offer updated");
        return saved;
    }

    private Offer getOfferForTenant(UUID offerId, UUID tenantId) {
        return offerRepository.findByIdAndTenantId(offerId, tenantId)
                .orElseThrow(() -> new OfferException("Offer not found."));
    }

    private void logAudit(UUID offerId, UUID tenantId, UUID actorId, String action, String oldStatus, String newStatus, String notes) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .offerId(offerId)
                    .tenantId(tenantId)
                    .actorId(actorId)
                    .action(action)
                    .oldStatus(oldStatus)
                    .newStatus(newStatus)
                    .notes(notes)
                    .build();
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            // Non-fatal logging failure
        }
    }
}
