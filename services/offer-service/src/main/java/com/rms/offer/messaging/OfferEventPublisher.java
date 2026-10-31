package com.rms.offer.messaging;

import com.rms.offer.messaging.event.OfferStatusChangedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

import com.rms.offer.messaging.event.OfferAcceptanceRetryEvent;
import com.rms.offer.messaging.event.OfferExpiredEvent;

@Service
public class OfferEventPublisher {
    
    private final RabbitTemplate rabbitTemplate;

    public OfferEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishOfferStatusChangedEvent(UUID offerId, UUID applicationId, UUID tenantId, String oldStatus, String newStatus) {
        OfferStatusChangedEvent event = new OfferStatusChangedEvent(offerId, applicationId, tenantId, oldStatus, newStatus);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, "offer.status.changed", event);
    }

    public void publishOfferAcceptanceRetryEvent(UUID offerId, UUID applicationId, UUID tenantId) {
        OfferAcceptanceRetryEvent event = new OfferAcceptanceRetryEvent(offerId, applicationId, tenantId);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, "offer.acceptance.retry", event);
    }

    public void publishOfferExpiredEvent(UUID offerId, UUID applicationId, UUID tenantId) {
        OfferExpiredEvent event = new OfferExpiredEvent(offerId, applicationId, tenantId);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, "offer.expired", event);
    }
}
