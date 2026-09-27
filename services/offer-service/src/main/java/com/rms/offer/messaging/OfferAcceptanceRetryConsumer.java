package com.rms.offer.messaging;

import com.rms.offer.client.AtsServiceClient;
import com.rms.offer.messaging.event.OfferAcceptanceRetryEvent;
import com.rms.offer.repository.OfferRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OfferAcceptanceRetryConsumer {

    private static final Logger log = LoggerFactory.getLogger(OfferAcceptanceRetryConsumer.class);

    private final AtsServiceClient atsServiceClient;
    private final OfferRepository offerRepository;

    public OfferAcceptanceRetryConsumer(AtsServiceClient atsServiceClient, OfferRepository offerRepository) {
        this.atsServiceClient = atsServiceClient;
        this.offerRepository = offerRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.OFFER_ACCEPTANCE_RETRY_QUEUE)
    public void onOfferAcceptanceRetry(OfferAcceptanceRetryEvent event) {
        if (event == null || event.offerId() == null || event.applicationId() == null) {
            return;
        }

        log.info("Processing offer acceptance retry for offerId={}, applicationId={}, tenantId={}",
                event.offerId(), event.applicationId(), event.tenantId());

        try {
            var offerOpt = offerRepository.findByIdAndTenantId(event.offerId(), event.tenantId());
            var hrAdminId = offerOpt.map(com.rms.offer.entity.Offer::getCreatedBy).orElse(null);
            atsServiceClient.hireApplication(event.applicationId(), event.tenantId(), hrAdminId);
            log.info("Successfully completed retry hire for applicationId={}", event.applicationId());
        } catch (Exception e) {
            log.error("Offer acceptance retry failed for offerId={}, applicationId={}: {}",
                    event.offerId(), event.applicationId(), e.getMessage());
            throw e; // throw so RabbitMQ retry or DLQ can handle it
        }
    }
}
