package com.recruitment.candidate.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(NotificationEvent event) {
        if (event == null) return;
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, "notification.event", event);
            log.info("Published notification event: {} to user: {}", event.actionType(), event.recipientUserId());
        } catch (Exception e) {
            log.warn("Failed to publish notification event {}: {}", event.actionType(), e.getMessage());
        }
    }
}
