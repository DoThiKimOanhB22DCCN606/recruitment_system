package com.rms.job.controller;

import com.rms.job.config.UserPrincipal;
import com.rms.job.dto.response.JobSummaryResponse;
import com.rms.job.dto.response.PageResponse;
import com.rms.job.dto.response.JobResponse;
import com.rms.job.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/employer/jobs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('HR_ADMIN', 'RECRUITER')")
public class EmployerJobController {

    private final JobService jobService;

    @GetMapping
    public ResponseEntity<PageResponse<JobSummaryResponse>> listForTenant(
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal.tenantId() == null) {
            throw new AccessDeniedException("Employer account must belong to a tenant");
        }
        return ResponseEntity.ok(PageResponse.from(jobService.listJobs(
                principal.tenantId(), principal.role(), companyId, PageRequest.of(page, size))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getForTenant(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal.tenantId() == null) {
            throw new AccessDeniedException("Employer account must belong to a tenant");
        }
        return ResponseEntity.ok(jobService.getJobById(id, principal.tenantId(), principal.role()));
    }
}
