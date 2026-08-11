package com.rms.iam.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

import com.rms.iam.messaging.event.EmailEvent;
import com.rms.iam.messaging.event.UserCreatedEvent;

@Service
public class IamEventPublisher {
    
    private final RabbitTemplate rabbitTemplate;

    public IamEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendVerificationEmail(String email, String rawToken) {
        EmailEvent event = new EmailEvent(email, rawToken, "VERIFICATION");
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, "iam.email.send", event);
    }
    
    public void sendPasswordResetEmail(String email, String rawToken) {
        EmailEvent event = new EmailEvent(email, rawToken, "PASSWORD_RESET");
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, "iam.email.send", event);
    }
    
    public void publishUserCreatedEvent(UUID userId, String email, String role, UUID tenantId) {
        // Since we don't have correlationId easily accessible here without HttpServletRequest, we generate or pass one
        // Better yet, use MDC.get("traceId") if available, or just a new UUID if absent.
        String correlationId = java.util.UUID.randomUUID().toString();
        UserCreatedEvent event = new UserCreatedEvent(
                "USER_REGISTERED", 
                userId, 
                email, 
                role, 
                tenantId, 
                correlationId, 
                java.time.Instant.now()
        );
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, "iam.user.created", event);
    }
    
    public void publishAccountLockedEvent(AccountLockedEvent event) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, "iam.account.locked", event);
    }
    
    public void publishSecurityAlertEvent(SecurityAlertEvent event) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, "iam.security.alert", event);
    }
}
