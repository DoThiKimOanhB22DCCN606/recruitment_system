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
public class AtsServiceClient {

    private final RestTemplate restTemplate;
    private final String atsServiceUrl;
    private final String internalSecret;

    public AtsServiceClient(RestTemplateBuilder builder,
                            @Value("${app.internal.ats-service-url:http://localhost:8084}") String atsServiceUrl,
                            @Value("${app.internal.secret:${rms.internal-secret:${INTERNAL_SECRET:uuid-internal-secret-12345}}}") String internalSecret) {
        this.restTemplate = builder.build();
        this.atsServiceUrl = atsServiceUrl;
        this.internalSecret = internalSecret;
    }

    public boolean isCvInUse(Object cvId) {
        try {
            String url = atsServiceUrl + "/internal/cvs/" + cvId + "/in-use";
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Secret", internalSecret);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            var response = restTemplate.exchange(url, HttpMethod.GET, request, Boolean.class);
            return Boolean.TRUE.equals(response.getBody());
        } catch (Exception e) {
            log.warn("Could not reach ATS service to check CV status: {}", e.getMessage());
            return false;
        }
    }
}
