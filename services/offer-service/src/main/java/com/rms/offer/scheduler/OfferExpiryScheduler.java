package com.rms.offer.scheduler;

import com.rms.offer.entity.Offer;
import com.rms.offer.entity.OfferStatus;
import com.rms.offer.repository.OfferRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
public class OfferExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(OfferExpiryScheduler.class);
    private final OfferRepository offerRepository;
    private final com.rms.offer.messaging.OfferEventPublisher offerEventPublisher;
    private final com.rms.offer.repository.AuditLogRepository auditLogRepository;

    public OfferExpiryScheduler(
            OfferRepository offerRepository,
            com.rms.offer.messaging.OfferEventPublisher offerEventPublisher,
            com.rms.offer.repository.AuditLogRepository auditLogRepository) {
        this.offerRepository = offerRepository;
        this.offerEventPublisher = offerEventPublisher;
        this.auditLogRepository = auditLogRepository;
    }

    @Scheduled(cron = "0 0 2 * * *", zone = "UTC") // Runs every day at 2 AM UTC
    @SchedulerLock(name = "expire_offers_task", lockAtLeastFor = "5m", lockAtMostFor = "15m")
    @Transactional
    public void expireOldOffers() {
        log.info("Running scheduled task: expireOldOffers");
        LocalDate today = LocalDate.now(java.time.ZoneOffset.UTC);
        List<Offer> expiredOffers = offerRepository.findExpiredSentOffers(today);
        
        if (expiredOffers.isEmpty()) {
            log.info("No offers to expire.");
            return;
        }

        for (Offer offer : expiredOffers) {
            String oldStatus = offer.getStatus().name();
            offer.setStatus(OfferStatus.EXPIRED);
            try {
                auditLogRepository.save(com.rms.offer.entity.AuditLog.builder()
                        .offerId(offer.getId())
                        .tenantId(offer.getTenantId())
                        .actorId(null)
                        .action("EXPIRE_OFFER")
                        .oldStatus(oldStatus)
                        .newStatus(OfferStatus.EXPIRED.name())
                        .notes("Offer expired automatically by scheduler")
                        .build());
            } catch (Exception ignored) {}
            offerEventPublisher.publishOfferExpiredEvent(offer.getId(), offer.getApplicationId(), offer.getTenantId());
            offerEventPublisher.publishOfferStatusChangedEvent(offer.getId(), offer.getApplicationId(), offer.getTenantId(), oldStatus, OfferStatus.EXPIRED.name());
        }
        
        offerRepository.saveAll(expiredOffers);
        log.info("Expired {} offers.", expiredOffers.size());
    }
}
