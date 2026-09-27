package com.recruitment.candidate.messaging;

import com.recruitment.candidate.entity.Candidate;
import com.recruitment.candidate.repository.CandidateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserCreatedEventListener {

    private final CandidateRepository candidateRepository;

    public record UserCreatedEvent(
            String eventType,
            UUID userId,
            String email,
            String role,
            UUID tenantId,
            String correlationId,
            Instant timestamp
    ) {}

    @Transactional
    @RabbitListener(queues = RabbitMQConfig.USER_CREATED_QUEUE)
    public void onUserCreated(UserCreatedEvent event) {
        if (event == null || event.userId() == null) {
            return;
        }

        // Only auto-create profile if candidate role or default
        if (event.role() != null && !event.role().equalsIgnoreCase("CANDIDATE") && !event.role().equalsIgnoreCase("ROLE_CANDIDATE")) {
            return;
        }

        UUID userUuid = event.userId();
        if (candidateRepository.findByUserUuid(userUuid).isEmpty()) {
            String initialName = event.email() != null ? event.email().split("@")[0] : "Candidate";
            Long legacyUserId = Math.abs(userUuid.getMostSignificantBits());
            Candidate candidate = Candidate.builder()
                    .userUuid(userUuid)
                    .userId(legacyUserId)
                    .email(event.email())
                    .fullName(initialName)
                    .build();
            candidateRepository.save(candidate);
            log.info("Auto-created candidate profile for userUuid: {}, email: {}", userUuid, event.email());
        }
    }
}
