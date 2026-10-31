package com.rms.company.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.UUID;

@Component
public class TenantClient {
    private final RestTemplate restTemplate;
    private final String iamServiceUrl;
    private final String internalSecret;

    public TenantClient(RestTemplateBuilder builder,
                        @Value("${app.internal.iam-service-url:${IAM_SERVICE_URL:http://localhost:8081}}") String iamServiceUrl,
                        @Value("${app.internal.secret:${rms.internal-secret:${INTERNAL_SECRET:uuid-internal-secret-12345}}}") String internalSecret) {
        this.restTemplate = builder.setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(5)).build();
        this.iamServiceUrl = iamServiceUrl;
        this.internalSecret = internalSecret;
    }

    public String getName(UUID tenantId) {
        TenantName response = restTemplate.exchange(url(tenantId), HttpMethod.GET,
                new HttpEntity<>(headers()), TenantName.class).getBody();
        if (response == null || response.name() == null || response.name().isBlank()) {
            throw new IllegalStateException("Tenant name is unavailable for " + tenantId);
        }
        return response.name();
    }

    public void updateName(UUID tenantId, String name) {
        restTemplate.exchange(url(tenantId) + "/name", HttpMethod.PUT,
                new HttpEntity<>(new TenantName(name), headers()), TenantName.class);
    }

    private String url(UUID tenantId) {
        return iamServiceUrl + "/internal/tenants/" + tenantId;
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Secret", internalSecret);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private record TenantName(String name) {}
}
