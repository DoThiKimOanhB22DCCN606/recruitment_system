package com.recruitment.job.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Slf4j
@Component
public class AtsClient {

    private final RestTemplate restTemplate;
    private final String atsServiceUrl;
    private final String internalSecret;

    public AtsClient(RestTemplateBuilder builder,
                     @Value("${app.internal.ats-service-url:http://localhost:8084}") String atsServiceUrl,
                     @Value("${app.internal.secret:${rms.internal-secret:${INTERNAL_SECRET:uuid-internal-secret-12345}}}") String internalSecret) {
        this.restTemplate = builder.build();
        this.atsServiceUrl = atsServiceUrl;
        this.internalSecret = internalSecret;
    }

    public void initializeDefaultStages(Long jobId, UUID tenantId) {
        try {
            String url = atsServiceUrl + "/internal/jobs/" + jobId + "/stages/default?tenantId=" + tenantId;
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Secret", internalSecret);
            if (tenantId != null) {
                headers.set("X-Tenant-Id", tenantId.toString());
            }
            HttpEntity<Void> request = new HttpEntity<>(headers);
            restTemplate.postForObject(url, request, Void.class);
            log.info("Initialized default ATS stages for job id: {}, tenant: {}", jobId, tenantId);
        } catch (Exception e) {
            log.warn("Could not call ATS service to initialize stages (will be created on demand or via event): {}", e.getMessage());
        }
    }
}
