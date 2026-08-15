package com.recruitment.ats.messaging;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.recruitment.ats.domain.Notification;
import com.recruitment.ats.repository.NotificationRepository;

@Component
public class NotificationListener {

    private final NotificationRepository repository;

    public NotificationListener(NotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    @KafkaListener(topics = "${app.kafka.notification-topic}", groupId = "ats-notification-store")
    public void consume(NotificationEvent event) {
        boolean duplicate = repository
                .existsByRecipientUserIdAndEntityIdAndActionTypeAndCreatedAtAfter(
                        event.recipientUserId(), event.entityId(), event.actionType(),
                        Instant.now().minus(5, ChronoUnit.MINUTES));
        if (duplicate) {
            return;
        }

        Notification notification = new Notification();
        notification.setTenantId(event.tenantId());
        notification.setRecipientUserId(event.recipientUserId());
        notification.setActionType(event.actionType());
        notification.setEntityType(event.entityType());
        notification.setEntityId(event.entityId());
        notification.setTitle(event.title());
        notification.setMessage(event.message());
        repository.save(notification);
    }
}
