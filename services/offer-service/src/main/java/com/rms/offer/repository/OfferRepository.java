package com.rms.offer.repository;

import com.rms.offer.entity.Offer;
import com.rms.offer.entity.OfferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OfferRepository extends JpaRepository<Offer, UUID> {
    Page<Offer> findAllByTenantId(UUID tenantId, Pageable pageable);
    Page<Offer> findAllByTenantIdAndCandidateId(UUID tenantId, UUID candidateId, Pageable pageable);
    List<Offer> findAllByStatus(OfferStatus status);
    
    java.util.Optional<Offer> findByIdAndTenantId(UUID id, UUID tenantId);
    List<Offer> findAllByCandidateIdAndTenantId(UUID candidateId, UUID tenantId);
    List<Offer> findAllByApplicationIdAndTenantId(UUID applicationId, UUID tenantId);
    
    @Query("SELECT o FROM Offer o WHERE o.status = com.rms.offer.entity.OfferStatus.SENT AND o.expiryDate < :date")
    List<Offer> findExpiredSentOffers(java.time.LocalDate date);
}
