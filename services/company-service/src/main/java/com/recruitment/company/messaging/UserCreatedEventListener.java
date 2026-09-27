package com.recruitment.company.messaging;

import com.recruitment.company.entity.Company;
import com.recruitment.company.repository.CompanyRepository;
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

    private final CompanyRepository companyRepository;

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
    @RabbitListener(queues = RabbitMQConfig.COMPANY_USER_CREATED_QUEUE)
    public void onUserCreated(UserCreatedEvent event) {
        if (event == null || event.userId() == null) {
            return;
        }

        // Only auto-create company if role is HR_ADMIN or RECRUITER or EMPLOYER
        if (event.role() == null || (!event.role().equalsIgnoreCase("HR_ADMIN") && !event.role().equalsIgnoreCase("RECRUITER") && !event.role().equalsIgnoreCase("ROLE_HR_ADMIN") && !event.role().equalsIgnoreCase("ROLE_RECRUITER"))) {
            return;
        }

        UUID ownerUserUuid = event.userId();
        if (companyRepository.findByOwnerUserUuid(ownerUserUuid).isEmpty()) {
            String companyName = (event.email() != null && event.email().contains("@"))
                    ? event.email().split("@")[1].split("\\.")[0].toUpperCase() + " Corp"
                    : "My Company";

            Long legacyOwnerUserId = Math.abs(ownerUserUuid.getMostSignificantBits());
            Company company = Company.builder()
                    .ownerUserUuid(ownerUserUuid)
                    .ownerUserId(legacyOwnerUserId)
                    .name(companyName)
                    .email(event.email())
                    .description("Welcome to " + companyName)
                    .build();
            companyRepository.save(company);
            log.info("Auto-created company profile for ownerUserUuid: {}, company: {}", ownerUserUuid, companyName);
        }
    }
}
