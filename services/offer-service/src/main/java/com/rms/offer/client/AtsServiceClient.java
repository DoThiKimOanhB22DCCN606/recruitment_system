package com.rms.offer.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Component
public class AtsServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AtsServiceClient.class);

    private final RestTemplate restTemplate;
    private final String atsServiceUrl;
    private final String internalSecret;

    public AtsServiceClient(
            RestTemplate restTemplate,
            @Value("${rms.ats-service-url}") String atsServiceUrl,
            @Value("${rms.internal-secret:${app.internal.secret:${INTERNAL_SECRET:uuid-internal-secret-12345}}}") String internalSecret) {
        this.restTemplate = restTemplate;
        this.atsServiceUrl = atsServiceUrl;
        this.internalSecret = internalSecret;
    }

    @CircuitBreaker(name = "atsClient", fallbackMethod = "validateApplicationFallback")
    public boolean validateApplication(UUID applicationId, UUID tenantId) {
        String url = String.format("%s/api/v1/internal/applications/%s/validate?tenantId=%s", atsServiceUrl, applicationId, tenantId);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Secret", internalSecret);
            if (tenantId != null) {
                headers.set("X-Tenant-Id", tenantId.toString());
            }
            HttpEntity<Void> request = new HttpEntity<>(headers);
            var response = restTemplate.exchange(url, HttpMethod.GET, request, Boolean.class);
            return Boolean.TRUE.equals(response.getBody());
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
        hireApplication(applicationId, tenantId, null);
    }

    @CircuitBreaker(name = "atsClient", fallbackMethod = "hireApplicationFallback")
    public void hireApplication(UUID applicationId, UUID tenantId, UUID hrAdminId) {
        String url = String.format("%s/internal/applications/%s/hire?tenantId=%s%s",
                atsServiceUrl,
                applicationId,
                tenantId != null ? tenantId : "",
                hrAdminId != null ? "&actorId=" + hrAdminId : "");
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Secret", internalSecret);
            if (tenantId != null) {
                headers.set("X-Tenant-Id", tenantId.toString());
            }
            if (hrAdminId != null) {
                headers.set("X-User-Id", hrAdminId.toString());
            }
            HttpEntity<Void> request = new HttpEntity<>(headers);
            restTemplate.postForObject(url, request, Void.class);
        } catch (Exception e) {
            log.error("Failed to hire application with ATS service", e);
            throw e;
        }
    }

    public void hireApplicationFallback(UUID applicationId, UUID tenantId, Throwable t) {
        hireApplicationFallback(applicationId, tenantId, null, t);
    }

    public void hireApplicationFallback(UUID applicationId, UUID tenantId, UUID hrAdminId, Throwable t) {
        log.warn("Circuit breaker fallback activated for ATS service hireApplication.", t);
        throw new RuntimeException("ATS service unavailable for hire action", t);
    }
}
