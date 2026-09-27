package com.recruitment.candidate.messaging;

import com.recruitment.candidate.service.CandidateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserAnonymizedEventListener {

    private final CandidateService candidateService;

    public record UserAnonymizedEvent(
            String eventType,
            UUID userId,
            String email,
            Instant timestamp
    ) {}

    @RabbitListener(queues = RabbitMQConfig.USER_ANONYMIZED_QUEUE)
    public void onUserAnonymized(UserAnonymizedEvent event) {
        if (event == null || event.userId() == null) {
            return;
        }
        log.info("Received UserAnonymizedEvent for userId: {}", event.userId());
        candidateService.anonymizeCandidate(event.userId());
    }
}
