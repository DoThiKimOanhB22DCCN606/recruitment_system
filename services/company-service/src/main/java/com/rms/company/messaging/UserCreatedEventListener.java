package com.rms.company.messaging;

import com.rms.company.client.TenantClient;
import com.rms.company.entity.Company;
import com.rms.company.repository.CompanyRepository;
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
    private final TenantClient tenantClient;

    public record UserCreatedEvent(
            String eventType,
            UUID userId,
            String email,
            String role,
            UUID tenantId,
            String correlationId,
            Instant occurredAt,
            String fullName
    ) {
        public UserCreatedEvent(String eventType, UUID userId, String email, String role,
                                UUID tenantId, String correlationId, Instant occurredAt) {
            this(eventType, userId, email, role, tenantId, correlationId, occurredAt, null);
        }
    }

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

        if (event.tenantId() == null || companyRepository.findByTenantId(event.tenantId()).isPresent()) {
            return;
        }

        UUID ownerUserUuid = event.userId();
        Company existing = companyRepository.findByOwnerUserUuid(ownerUserUuid).orElse(null);
        String companyName = tenantClient.getName(event.tenantId());
        if (existing != null && existing.getTenantId() == null) {
            existing.setTenantId(event.tenantId());
            existing.setName(companyName);
            companyRepository.save(existing);
            return;
        }
        if (existing == null) {
            Long legacyOwnerUserId = Math.abs(ownerUserUuid.getMostSignificantBits());
            Company company = Company.builder()
                    .tenantId(event.tenantId())
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
