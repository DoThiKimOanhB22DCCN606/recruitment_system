package com.recruitment.job.controller;

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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
@Tag(name = "Jobs", description = "Job posting management, approval workflow and search")
public class JobController {

    private final JobService jobService;

    @PostMapping
    @Operation(summary = "Create a new job posting (status = DRAFT)")
    public ResponseEntity<JobResponse> create(@Valid @RequestBody JobCreateRequest request,
                                               @AuthenticationPrincipal Jwt jwt) {
        Long employerId = extractUserId(jwt);
        JobResponse response = jobService.createJob(request, employerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List job postings, optionally filtered by company")
    public ResponseEntity<PageResponse<JobSummaryResponse>> list(
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(PageResponse.from(jobService.listJobs(companyId, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get job posting detail")
    public ResponseEntity<JobResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(jobService.getJobById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a job posting")
    public ResponseEntity<JobResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody JobUpdateRequest request,
                                               @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(jobService.updateJob(id, request, extractUserId(jwt)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a job posting")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        jobService.deleteJob(id, extractUserId(jwt));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @Operation(summary = "Full-text search + filters over PUBLISHED jobs")
    public ResponseEntity<PageResponse<JobSummaryResponse>> search(JobSearchRequest request) {
        return ResponseEntity.ok(PageResponse.from(jobService.search(request)));
    }

    @PostMapping("/{id}/status")
    @Operation(summary = "Transition a job's approval status (Draft -> Pending -> Approved -> Published -> Closed)")
    public ResponseEntity<JobResponse> changeStatus(@PathVariable Long id,
                                                     @Valid @RequestBody JobStatusChangeRequest request,
                                                     @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(jobService.changeStatus(id, request, extractUserId(jwt)));
    }

    @GetMapping("/{id}/status-history")
    @Operation(summary = "Get the approval/status change history of a job")
    public ResponseEntity<List<JobStatusHistoryResponse>> statusHistory(@PathVariable Long id) {
        return ResponseEntity.ok(jobService.getStatusHistory(id));
    }

    private Long extractUserId(Jwt jwt) {
        if (jwt == null) return null; // allows local testing without a token
        Object sub = jwt.getClaim("userId");
        return sub != null ? Long.valueOf(sub.toString()) : Long.valueOf(jwt.getSubject());
    }
}
