package com.rms.ats.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.rms.ats.config.RequestContext;

@Component
public class InternalServiceClient {

    private final RestClient restClient;
    private final String jobServiceUrl;
    private final String profileServiceUrl;
    private final String iamServiceUrl;
    private final String internalSecret;
    private final boolean mock;

    public InternalServiceClient(
            RestClient.Builder builder,
            @Value("${app.internal.job-service-url:http://localhost:8083}") String jobServiceUrl,
            @Value("${app.internal.profile-service-url:http://localhost:8082}") String profileServiceUrl,
            @Value("${app.internal.iam-service-url:http://localhost:8081}") String iamServiceUrl,
            @Value("${app.internal.secret:${rms.internal-secret:${INTERNAL_SECRET:uuid-internal-secret-12345}}}") String internalSecret,
            @Value("${app.internal.mock:true}") boolean mock) {
        this.restClient = builder.build();
        this.jobServiceUrl = jobServiceUrl;
        this.profileServiceUrl = profileServiceUrl;
        this.iamServiceUrl = iamServiceUrl;
        this.internalSecret = internalSecret;
        this.mock = mock;
    }

    public java.util.List<UUID> getTenantRecruiterIds(UUID tenantId) {
        if (mock || tenantId == null) {
            return java.util.List.of();
        }
        try {
            UUID[] ids = restClient.get()
                    .uri(iamServiceUrl + "/internal/tenants/{tenantId}/recruiters", tenantId)
                    .header("X-Internal-Secret", internalSecret)
                    .retrieve()
                    .body(UUID[].class);
            return ids != null ? java.util.List.of(ids) : java.util.List.of();
        } catch (Exception e) {
            return java.util.List.of();
        }
    }

    public UserIdentity getUserIdentity(UUID userId) {
        if (mock) {
            return null;
        }
        return restClient.get()
                .uri(iamServiceUrl + "/internal/users/{userId}/identity", userId)
                .header("X-Internal-Secret", internalSecret)
                .retrieve()
                .body(UserIdentity.class);
    }

    @Cacheable(value = "job-status", key = "#jobId")
    public JobStatus getJobStatus(Long jobId) {
        if (mock) {
            return new JobStatus(jobId,
                    UUID.fromString("00000000-0000-4000-8000-000000000100"), "PUBLISHED",
                    UUID.fromString("00000000-0000-4000-8000-000000000002"));
        }
        return restClient.get()
                .uri(jobServiceUrl + "/internal/jobs/{jobId}/status", jobId)
                .header("X-Internal-Secret", internalSecret)
                .retrieve()
                .body(JobStatus.class);
    }

    public JobStatus getJobStatus(UUID jobId) {
        return getJobStatus(jobId.getLeastSignificantBits());
    }

    @Cacheable(value = "cv-details", key = "#cvId + ':' + #candidateId")
    public CvDetails getCv(Long cvId, UUID candidateId) {
        if (mock) {
            return new CvDetails(cvId, candidateId, null, null, null, null, true, null);
        }
        return restClient.get()
                .uri(profileServiceUrl + "/internal/cvs/{cvId}", cvId)
                .header("X-Internal-Secret", internalSecret)
                .header("X-User-Id", (RequestContext.currentUserId() != null
                        ? RequestContext.currentUserId() : candidateId).toString())
                .retrieve()
                .body(CvDetails.class);
    }

    public byte[] downloadCv(Long cvId) {
        if (mock) {
            return new byte[0];
        }
        return restClient.get()
                .uri(profileServiceUrl + "/internal/cvs/{cvId}/file", cvId)
                .header("X-Internal-Secret", internalSecret)
                .header("X-User-Id", RequestContext.userId().toString())
                .retrieve()
                .body(byte[].class);
    }

    public CvDetails getCv(UUID cvId, UUID candidateId) {
        return getCv(cvId.getLeastSignificantBits(), candidateId);
    }

    public record JobStatus(Long jobId, UUID tenantId, String status, UUID recruiterId, String title) {
        public JobStatus(Long jobId, UUID tenantId, String status, UUID recruiterId) {
            this(jobId, tenantId, status, recruiterId, null);
        }
    }

    public record UserIdentity(UUID userId, String email, String role, String fullName) {
    }

    public record CvDetails(
            Long cvId,
            UUID candidateId,
            String candidateName,
            String candidateEmail,
            String fileName,
            String downloadUrl,
            boolean valid,
            String contentType) {
        public CvDetails(Long cvId, UUID candidateId) {
            this(cvId, candidateId, null, null, null, null, true, null);
        }
    }
}
