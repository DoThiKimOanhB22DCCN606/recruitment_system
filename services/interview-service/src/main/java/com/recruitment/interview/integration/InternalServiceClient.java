package com.recruitment.interview.integration;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.recruitment.interview.config.RequestContext;

@Component
public class InternalServiceClient {

    private final RestClient restClient;
    private final String atsServiceUrl;
    private final String userServiceUrl;
    private final boolean mockUsers;

    public InternalServiceClient(
            RestClient.Builder builder,
            @Value("${app.internal.ats-service-url}") String atsServiceUrl,
            @Value("${app.internal.user-service-url}") String userServiceUrl,
            @Value("${app.internal.mock-users:true}") boolean mockUsers) {
        this.restClient = builder.build();
        this.atsServiceUrl = atsServiceUrl;
        this.userServiceUrl = userServiceUrl;
        this.mockUsers = mockUsers;
    }

    public ApplicationDetails getApplication(UUID applicationId) {
        return restClient.get()
                .uri(atsServiceUrl + "/internal/applications/{id}", applicationId)
                .header("X-User-Id", RequestContext.userId().toString())
                .header("X-Tenant-Id", RequestContext.requireTenantId().toString())
                .header("X-User-Role", RequestContext.role())
                .retrieve()
                .body(ApplicationDetails.class);
    }

    @Cacheable(value = "users", key = "#userId + ':' + #tenantId")
    public UserDetails getUser(UUID userId, UUID tenantId) {
        if (mockUsers) {
            return new UserDetails(userId, tenantId, "RECRUITER");
        }
        return restClient.get()
                .uri(userServiceUrl + "/internal/users/{id}", userId)
                .header("X-Tenant-Id", tenantId.toString())
                .retrieve()
                .body(UserDetails.class);
    }

    public record ApplicationDetails(
            UUID id,
            UUID tenantId,
            UUID jobId,
            UUID candidateId,
            UUID cvId,
            String coverLetter,
            String status,
            UUID currentStageId,
            String currentStageName) {
    }

    public record UserDetails(UUID id, UUID tenantId, String role) {
    }
}
