package com.rms.ats.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.rms.ats.entity.Notification;
import com.rms.ats.repository.NotificationRepository;

@Component
public class NotificationListener {

    private final NotificationRepository repository;

    public NotificationListener(NotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    @KafkaListener(topics = "${app.kafka.notification-topic}", groupId = "ats-notification-store")
    public void consume(NotificationEvent event) {
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

    @Transactional
    @RabbitListener(queues = RabbitMQConfig.COMPANY_REGISTRATION_QUEUE)
    public void consumeCompanyRegistration(CompanyRegistrationSubmittedEvent event) {
        consume(new NotificationEvent(null, event.recipientUserId(),
                "COMPANY_REGISTRATION_SUBMITTED", "COMPANY_REGISTRATION", event.registrationId(),
                "Doanh nghiệp mới chờ duyệt",
                event.companyName() + " (" + event.applicantEmail() + ") đã xác minh email và đang chờ duyệt."));
    }
}
