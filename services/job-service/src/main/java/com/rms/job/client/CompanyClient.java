package com.rms.job.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.UUID;

@Component
public class CompanyClient {

    private final RestTemplate restTemplate;
    private final String companyServiceUrl;
    private final String internalSecret;

    public CompanyClient(RestTemplateBuilder builder,
                         @Value("${app.internal.company-service-url:${COMPANY_SERVICE_URL:http://localhost:8087}}") String companyServiceUrl,
                         @Value("${app.internal.secret:${rms.internal-secret:${INTERNAL_SECRET:uuid-internal-secret-12345}}}") String internalSecret) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
        this.companyServiceUrl = companyServiceUrl;
        this.internalSecret = internalSecret;
    }

    public CompanyInfo getCompanyByTenant(UUID tenantId, UUID userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Secret", internalSecret);
        String url = companyServiceUrl + "/internal/company/tenant/" + tenantId + "/resolve?userId=" + userId;
        CompanyInfo company = restTemplate.exchange(url, HttpMethod.POST,
                new HttpEntity<>(headers), CompanyInfo.class).getBody();
        if (company == null || company.id() == null) {
            throw new IllegalStateException("Company profile is unavailable for tenant " + tenantId);
        }
        return company;
    }

    public record CompanyInfo(Long id, String name, String logoUrl, boolean verified) {}
}
