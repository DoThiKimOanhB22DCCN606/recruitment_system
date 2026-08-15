package com.recruitment.ats.integration;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.recruitment.ats.config.RequestContext;

@Component
public class InternalServiceClient {

    private final RestClient restClient;
    private final String jobServiceUrl;
    private final String profileServiceUrl;
    private final boolean mock;

    public InternalServiceClient(
            RestClient.Builder builder,
            @Value("${app.internal.job-service-url}") String jobServiceUrl,
            @Value("${app.internal.profile-service-url}") String profileServiceUrl,
            @Value("${app.internal.mock:true}") boolean mock) {
        this.restClient = builder.build();
        this.jobServiceUrl = jobServiceUrl;
        this.profileServiceUrl = profileServiceUrl;
        this.mock = mock;
    }

    @Cacheable(value = "job-status", key = "#jobId")
    public JobStatus getJobStatus(UUID jobId) {
        if (mock) {
            return new JobStatus(jobId,
                    UUID.fromString("00000000-0000-4000-8000-000000000100"), "PUBLISHED",
                    UUID.fromString("00000000-0000-4000-8000-000000000002"));
        }
        return restClient.get()
                .uri(jobServiceUrl + "/internal/jobs/{jobId}/status", jobId)
                .retrieve()
                .body(JobStatus.class);
    }

    @Cacheable(value = "cv-details", key = "#cvId + ':' + #candidateId")
    public CvDetails getCv(UUID cvId, UUID candidateId) {
        if (mock) {
            return new CvDetails(cvId, candidateId);
        }
        return restClient.get()
                .uri(profileServiceUrl + "/internal/cvs/{cvId}", cvId)
                .header("X-User-Id", RequestContext.userId().toString())
                .retrieve()
                .body(CvDetails.class);
    }

    public record JobStatus(UUID jobId, UUID tenantId, String status, UUID recruiterId) {
    }

    public record CvDetails(UUID id, UUID candidateId) {
    }
}
