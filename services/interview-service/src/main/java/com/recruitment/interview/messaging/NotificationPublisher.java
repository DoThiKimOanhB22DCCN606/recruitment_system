package com.recruitment.interview.messaging;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationPublisher {

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;
    private final String topic;

    public NotificationPublisher(
            KafkaTemplate<String, NotificationEvent> kafkaTemplate,
            @Value("${app.kafka.notification-topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(NotificationEvent event) {
        kafkaTemplate.send(topic, event.recipientUserId().toString(), event);
    }
}
