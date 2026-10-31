package com.rms.interview.client;

import java.util.UUID;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.rms.interview.config.RequestContext;

@Component
public class InternalServiceClient {

    private final RestClient restClient;
    private final String atsServiceUrl;
    private final String userServiceUrl;
    private final String internalSecret;
    private final boolean mockUsers;

    public InternalServiceClient(
            RestClient.Builder builder,
            @Value("${app.internal.ats-service-url}") String atsServiceUrl,
            @Value("${app.internal.user-service-url}") String userServiceUrl,
            @Value("${app.internal.secret:${rms.internal-secret:${INTERNAL_SECRET:uuid-internal-secret-12345}}}") String internalSecret,
            @Value("${app.internal.mock-users:true}") boolean mockUsers) {
        this.restClient = builder.build();
        this.atsServiceUrl = atsServiceUrl;
        this.userServiceUrl = userServiceUrl;
        this.internalSecret = internalSecret;
        this.mockUsers = mockUsers;
    }

    public ApplicationDetails getApplication(UUID applicationId) {
        return restClient.get()
                .uri(atsServiceUrl + "/internal/applications/{id}", applicationId)
                .header("X-Internal-Secret", internalSecret)
                .header("X-User-Id", RequestContext.userId().toString())
                .header("X-Tenant-Id", RequestContext.requireTenantId().toString())
                .header("X-User-Role", RequestContext.role())
                .retrieve()
                .body(ApplicationDetails.class);
    }

    public List<UUID> getCandidateApplicationIds(UUID candidateId) {
        List<UUID> ids = restClient.get()
                .uri(atsServiceUrl + "/internal/candidates/{candidateId}/application-ids", candidateId)
                .header("X-Internal-Secret", internalSecret)
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<List<UUID>>() {});
        return ids == null ? List.of() : ids;
    }

    @Cacheable(value = "users", key = "#userId + ':' + #tenantId")
    public UserDetails getUser(UUID userId, UUID tenantId) {
        if (mockUsers) {
            return new UserDetails(userId, tenantId, "RECRUITER");
        }
        return restClient.get()
                .uri(userServiceUrl + "/internal/tenants/{tenantId}/users/{userId}", tenantId, userId)
                .header("X-Internal-Secret", internalSecret)
                .retrieve()
                .body(UserDetails.class);
    }

    public UserDetails getUserByEmail(String email, UUID tenantId) {
        if (mockUsers) {
            return new UserDetails(UUID.randomUUID(), tenantId, "RECRUITER");
        }
        java.net.URI uri = org.springframework.web.util.UriComponentsBuilder
                .fromUriString(userServiceUrl)
                .path("/internal/tenants/{tenantId}/users/by-email")
                .queryParam("email", "{email}")
                .encode()
                .buildAndExpand(tenantId, email)
                .toUri();
        return restClient.get()
                .uri(uri)
                .header("X-Internal-Secret", internalSecret)
                .retrieve()
                .body(UserDetails.class);
    }

    public record ApplicationDetails(
            UUID id,
            UUID tenantId,
            Long jobId,
            UUID candidateId,
            Long cvId,
            String coverLetter,
            String status,
            UUID currentStageId,
            String currentStageName,
            String candidateName,
            String candidateEmail) {
        public ApplicationDetails(UUID id, UUID tenantId, Long jobId, UUID candidateId,
                                  Long cvId, String coverLetter, String status,
                                  UUID currentStageId, String currentStageName) {
            this(id, tenantId, jobId, candidateId, cvId, coverLetter, status,
                    currentStageId, currentStageName, null, null);
        }
    }

    public record UserDetails(UUID id, UUID tenantId, String role, String fullName, String email) {
        public UserDetails(UUID id, UUID tenantId, String role) {
            this(id, tenantId, role, null, null);
        }
    }
}
