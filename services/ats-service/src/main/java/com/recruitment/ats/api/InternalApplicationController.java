package com.recruitment.ats.api;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.recruitment.ats.api.ApplicationDtos.ApplicationResponse;
import com.recruitment.ats.service.ApplicationService;

@RestController
@RequestMapping
public class InternalApplicationController {

    private final ApplicationService service;

    public InternalApplicationController(ApplicationService service) {
        this.service = service;
    }

    @GetMapping({"/internal/applications/{id}", "/api/v1/internal/applications/{id}"})
    public ApplicationResponse get(@PathVariable UUID id) {
        return service.getInternal(id);
    }

    @PostMapping({"/internal/applications/{id}/hire", "/api/v1/internal/applications/{id}/hire"})
    public ResponseEntity<Void> hire(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tenant-Id", required = false) UUID tenantIdHeader,
            @RequestParam(value = "tenantId", required = false) UUID tenantIdParam,
            @RequestHeader(value = "X-User-Id", required = false) UUID actorIdHeader,
            @RequestParam(value = "actorId", required = false) UUID actorIdParam) {
        UUID tenantId = tenantIdHeader != null ? tenantIdHeader : tenantIdParam;
        UUID actorId = actorIdHeader != null ? actorIdHeader : actorIdParam;
        service.hireInternal(id, tenantId, actorId);
        return ResponseEntity.ok().build();
    }

    @GetMapping({"/internal/applications/{id}/validate", "/api/v1/internal/applications/{id}/validate"})
    public ResponseEntity<Boolean> validate(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tenant-Id", required = false) UUID tenantIdHeader,
            @RequestParam(value = "tenantId", required = false) UUID tenantIdParam) {
        UUID tenantId = tenantIdHeader != null ? tenantIdHeader : tenantIdParam;
        return ResponseEntity.ok(service.validateInternal(id, tenantId));
    }

    @GetMapping({"/internal/cvs/{cvId}/in-use", "/api/v1/internal/cvs/{cvId}/in-use"})
    public ResponseEntity<Boolean> isCvInUse(@PathVariable UUID cvId) {
        return ResponseEntity.ok(service.isCvInUse(cvId));
    }

    @PostMapping({"/internal/jobs/{jobId}/stages/default", "/api/v1/internal/jobs/{jobId}/stages/default"})
    public ResponseEntity<Void> createDefaultStages(
            @PathVariable String jobId,
            @RequestHeader(value = "X-Tenant-Id", required = false) UUID tenantIdHeader,
            @RequestParam(value = "tenantId", required = false) UUID tenantIdParam) {
        UUID tenantId = tenantIdHeader != null ? tenantIdHeader : tenantIdParam;
        service.createDefaultStagesForJob(jobId, tenantId);
        return ResponseEntity.ok().build();
    }
}

