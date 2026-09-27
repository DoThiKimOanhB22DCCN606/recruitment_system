package com.recruitment.job.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishJobCreated(Long jobId, UUID tenantId, String title, UUID recruiterId) {
        try {
            JobCreatedEvent event = new JobCreatedEvent(jobId, tenantId, title, recruiterId);
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, "job.created", event);
            log.info("Published job.created event for job id: {}", jobId);
        } catch (Exception e) {
            log.warn("Failed to publish job.created event: {}", e.getMessage());
        }
    }

    public void publishApprovalOutcome(Long jobId, UUID tenantId, UUID recruiterId, String status, String note) {
        try {
            JobApprovalOutcomeEvent event = new JobApprovalOutcomeEvent(jobId, tenantId, recruiterId, status, note);
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, "job.approval.outcome", event);
            log.info("Published job.approval.outcome event for job id: {}, status: {}", jobId, status);
        } catch (Exception e) {
            log.warn("Failed to publish job.approval.outcome event: {}", e.getMessage());
        }
    }

    public record JobCreatedEvent(Long jobId, UUID tenantId, String title, UUID recruiterId) {}
    public record JobApprovalOutcomeEvent(Long jobId, UUID tenantId, UUID recruiterId, String status, String note) {}
}
