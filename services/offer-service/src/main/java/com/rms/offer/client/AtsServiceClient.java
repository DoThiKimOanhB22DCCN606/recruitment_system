package com.rms.offer.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Component
public class AtsServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AtsServiceClient.class);

    private final RestTemplate restTemplate;
    private final String atsServiceUrl;

    public AtsServiceClient(RestTemplate restTemplate, @Value("${rms.ats-service-url}") String atsServiceUrl) {
        this.restTemplate = restTemplate;
        this.atsServiceUrl = atsServiceUrl;
    }

    @CircuitBreaker(name = "atsClient", fallbackMethod = "validateApplicationFallback")
    public boolean validateApplication(UUID applicationId, UUID tenantId) {
        String url = String.format("%s/api/v1/internal/applications/%s/validate?tenantId=%s", atsServiceUrl, applicationId, tenantId);
        try {
            Boolean isValid = restTemplate.getForObject(url, Boolean.class);
            return isValid != null && isValid;
        } catch (Exception e) {
            log.error("Failed to validate application with ATS service", e);
            throw e; // throw to trigger circuit breaker
        }
    }

    public boolean validateApplicationFallback(UUID applicationId, UUID tenantId, Throwable t) {
        log.warn("Circuit breaker fallback activated for ATS service validateApplication. Rejecting validation.", t);
        return false;
    }

    @CircuitBreaker(name = "atsClient", fallbackMethod = "hireApplicationFallback")
    public void hireApplication(UUID applicationId, UUID tenantId) {
        String url = String.format("%s/internal/applications/%s/hire?tenantId=%s", atsServiceUrl, applicationId, tenantId);
        try {
            restTemplate.postForObject(url, null, Void.class);
        } catch (Exception e) {
            log.error("Failed to hire application with ATS service", e);
            throw e;
        }
    }

    public void hireApplicationFallback(UUID applicationId, UUID tenantId, Throwable t) {
        log.warn("Circuit breaker fallback activated for ATS service hireApplication.", t);
        throw new RuntimeException("ATS service unavailable for hire action", t);
    }
}
