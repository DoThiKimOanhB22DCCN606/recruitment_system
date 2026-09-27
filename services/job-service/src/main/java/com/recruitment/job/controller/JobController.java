package com.recruitment.job.controller;

import com.recruitment.job.config.UserPrincipal;
import com.recruitment.job.dto.request.*;
import com.recruitment.job.dto.response.*;
import com.recruitment.job.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/jobs", "/jobs"})
@RequiredArgsConstructor
@Tag(name = "Jobs", description = "Job posting management, approval workflow and search")
public class JobController {

    private final JobService jobService;

    @PostMapping
    @Operation(summary = "Create a new job posting")
    public ResponseEntity<JobResponse> create(
            @Valid @RequestBody JobCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-User-Id", required = false) UUID headerUserId,
            @RequestHeader(value = "X-Tenant-Id", required = false) UUID headerTenantId,
            @RequestHeader(value = "X-User-Role", required = false) String headerRole) {
        
        UUID userId = principal != null ? principal.userId() : headerUserId;
        UUID tenantId = principal != null ? principal.tenantId() : headerTenantId;
        String role = principal != null ? principal.role() : headerRole;

        JobResponse response = jobService.createJob(request, userId, tenantId, role);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List job postings")
    public ResponseEntity<PageResponse<JobSummaryResponse>> list(
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Tenant-Id", required = false) UUID headerTenantId,
            @RequestHeader(value = "X-User-Role", required = false) String headerRole) {
        
        UUID tenantId = principal != null ? principal.tenantId() : headerTenantId;
        String role = principal != null ? principal.role() : headerRole;

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(PageResponse.from(jobService.listJobs(tenantId, role, companyId, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get job posting detail")
    public ResponseEntity<JobResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Tenant-Id", required = false) UUID headerTenantId,
            @RequestHeader(value = "X-User-Role", required = false) String headerRole) {
        
        UUID tenantId = principal != null ? principal.tenantId() : headerTenantId;
        String role = principal != null ? principal.role() : headerRole;

        return ResponseEntity.ok(jobService.getJobById(id, tenantId, role));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a job posting")
    public ResponseEntity<JobResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody JobUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-User-Id", required = false) UUID headerUserId,
            @RequestHeader(value = "X-Tenant-Id", required = false) UUID headerTenantId,
            @RequestHeader(value = "X-User-Role", required = false) String headerRole) {
        
        UUID userId = principal != null ? principal.userId() : headerUserId;
        UUID tenantId = principal != null ? principal.tenantId() : headerTenantId;
        String role = principal != null ? principal.role() : headerRole;

        return ResponseEntity.ok(jobService.updateJob(id, request, userId, tenantId, role));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a job posting")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-User-Id", required = false) UUID headerUserId,
            @RequestHeader(value = "X-Tenant-Id", required = false) UUID headerTenantId,
            @RequestHeader(value = "X-User-Role", required = false) String headerRole) {
        
        UUID userId = principal != null ? principal.userId() : headerUserId;
        UUID tenantId = principal != null ? principal.tenantId() : headerTenantId;
        String role = principal != null ? principal.role() : headerRole;

        jobService.deleteJob(id, userId, tenantId, role);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @Operation(summary = "Full-text search + filters over PUBLISHED jobs")
    public ResponseEntity<PageResponse<JobSummaryResponse>> search(JobSearchRequest request) {
        return ResponseEntity.ok(PageResponse.from(jobService.search(request)));
    }

    @PostMapping("/{id}/status")
    @Operation(summary = "Transition a job's approval status")
    public ResponseEntity<JobResponse> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody JobStatusChangeRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-User-Id", required = false) UUID headerUserId,
            @RequestHeader(value = "X-Tenant-Id", required = false) UUID headerTenantId,
            @RequestHeader(value = "X-User-Role", required = false) String headerRole) {
        
        UUID userId = principal != null ? principal.userId() : headerUserId;
        UUID tenantId = principal != null ? principal.tenantId() : headerTenantId;
        String role = principal != null ? principal.role() : headerRole;

        return ResponseEntity.ok(jobService.changeStatus(id, request, userId, tenantId, role));
    }

    @GetMapping("/{id}/status-history")
    @Operation(summary = "Get the approval/status change history of a job")
    public ResponseEntity<List<JobStatusHistoryResponse>> statusHistory(@PathVariable Long id) {
        return ResponseEntity.ok(jobService.getStatusHistory(id));
    }
}
