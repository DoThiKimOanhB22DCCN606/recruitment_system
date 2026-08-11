package com.rms.offer.controller;

import com.rms.offer.dto.OfferRequest;
import com.rms.offer.entity.Offer;
import com.rms.offer.service.OfferService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/offers")
public class OfferController {

    private final OfferService offerService;

    public OfferController(OfferService offerService) {
        this.offerService = offerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('HR_ADMIN')")
    public Offer createDraftOffer(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody OfferRequest request) {
        return offerService.createDraftOffer(request, tenantId, userId);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('HR_ADMIN') or hasAuthority('CANDIDATE') or hasAuthority('INTERVIEWER')")
    public List<Offer> getOffers(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestParam(required = false) UUID candidateId,
            @RequestParam(required = false) UUID applicationId) {
        
        if ("CANDIDATE".equals(userRole) || "ROLE_CANDIDATE".equals(userRole)) {
            candidateId = userId;
        }
        
        return offerService.getOffers(tenantId, candidateId, applicationId);
    }

    @GetMapping("/{offerId}")
    @PreAuthorize("hasAuthority('HR_ADMIN') or hasAuthority('CANDIDATE')")
    public Offer getOffer(
            @PathVariable UUID offerId,
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        
        Offer offer = offerService.getOffer(offerId, tenantId);
        
        if ("CANDIDATE".equals(userRole) || "ROLE_CANDIDATE".equals(userRole)) {
            if (!userId.equals(offer.getCandidateId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
            }
        }
        
        return offer;
    }

    @PatchMapping("/{offerId}")
    @PreAuthorize("hasAuthority('HR_ADMIN')")
    public Offer updateOffer(
            @PathVariable UUID offerId,
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody OfferRequest request) {
        return offerService.updateOffer(offerId, tenantId, request);
    }

    @PatchMapping("/{offerId}/send")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('HR_ADMIN')")
    public void sendOffer(
            @PathVariable UUID offerId,
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        offerService.sendOffer(offerId, tenantId);
    }

    @PostMapping("/{offerId}/accept")
    @PreAuthorize("hasAuthority('CANDIDATE')")
    public ResponseEntity<Void> acceptOffer(
            @PathVariable UUID offerId,
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader("X-User-Id") UUID userId) {
        boolean success = offerService.acceptOffer(offerId, tenantId, userId);
        if (success) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.accepted().build();
        }
    }

    @PostMapping("/{offerId}/decline")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('CANDIDATE')")
    public void declineOffer(
            @PathVariable UUID offerId,
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader("X-User-Id") UUID userId) {
        offerService.declineOffer(offerId, tenantId, userId);
    }

    @PatchMapping("/{offerId}/withdraw")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('HR_ADMIN')")
    public void withdrawOffer(
            @PathVariable UUID offerId,
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        offerService.withdrawOffer(offerId, tenantId);
    }
}
