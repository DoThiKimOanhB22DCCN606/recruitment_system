package com.recruitment.job.controller;

import com.recruitment.job.dto.response.JobInternalStatusResponse;
import com.recruitment.job.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal, service-to-service API. Not exposed publicly through the API
 * gateway; consumed by the ATS Service to check whether a job still accepts
 * applications before creating/advancing an application record.
 */
@RestController
@RequestMapping("/internal/jobs")
@RequiredArgsConstructor
@Tag(name = "Internal - Jobs")
public class InternalJobController {

    private final JobService jobService;

    @GetMapping("/{id}/status")
    @Operation(summary = "Get current status of a job (used by ATS Service)")
    public JobInternalStatusResponse getStatus(@PathVariable Long id) {
        return jobService.getInternalStatus(id);
    }
}
