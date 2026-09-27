package com.recruitment.candidate.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Slf4j
@Component
public class IamServiceClient {

    private final RestTemplate restTemplate;
    private final String iamServiceUrl;
    private final String internalSecret;

    public IamServiceClient(RestTemplateBuilder builder,
                            @Value("${app.internal.iam-service-url:http://localhost:8081}") String iamServiceUrl,
                            @Value("${app.internal.secret:${rms.internal-secret:${INTERNAL_SECRET:uuid-internal-secret-12345}}}") String internalSecret) {
        this.restTemplate = builder.build();
        this.iamServiceUrl = iamServiceUrl;
        this.internalSecret = internalSecret;
    }

    public void deactivateUser(UUID userId) {
        if (userId == null) return;
        try {
            String url = iamServiceUrl + "/internal/users/" + userId + "/deactivate";
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Secret", internalSecret);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            restTemplate.exchange(url, HttpMethod.POST, request, Void.class);
            log.info("Deactivated user {} in IAM service", userId);
        } catch (Exception e) {
            log.warn("Could not reach IAM service to deactivate user {}: {}", userId, e.getMessage());
        }
    }
}
