package com.rms.offer.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class OfferEmailContextClient {
    private final RestTemplate rest;
    private final String atsUrl;
    private final String jobUrl;
    private final String companyUrl;
    private final String secret;

    public OfferEmailContextClient(RestTemplate rest,
            @Value("${rms.ats-service-url}") String atsUrl,
            @Value("${rms.job-service-url}") String jobUrl,
            @Value("${rms.company-service-url}") String companyUrl,
            @Value("${rms.internal-secret}") String secret) {
        this.rest = rest;
        this.atsUrl = atsUrl;
        this.jobUrl = jobUrl;
        this.companyUrl = companyUrl;
        this.secret = secret;
    }

    public Context get(UUID applicationId, UUID tenantId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Secret", secret);
        headers.set("X-Tenant-Id", tenantId.toString());
        Application app = rest.exchange(atsUrl + "/internal/applications/" + applicationId,
                org.springframework.http.HttpMethod.GET, new HttpEntity<Void>(headers), Application.class).getBody();
        if (app == null || app.jobId() == null) throw new IllegalStateException("Không lấy được hồ sơ ứng tuyển");
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        Job job = rest.exchange(jobUrl + "/internal/jobs/" + app.jobId(),
                org.springframework.http.HttpMethod.GET, entity, Job.class).getBody();
        Brand brand = rest.getForObject(companyUrl + "/api/v1/companies/tenant/" + tenantId + "/branding", Brand.class);
        return new Context(app.candidateName(), app.candidateEmail(), job == null ? null : job.title(),
                brand == null ? null : brand.name());
    }

    public record Application(String candidateName, String candidateEmail, Long jobId) {}
    public record Job(String title) {}
    public record Brand(String name) {}
    public record Context(String candidateName, String candidateEmail, String jobTitle, String companyName) {}
}
