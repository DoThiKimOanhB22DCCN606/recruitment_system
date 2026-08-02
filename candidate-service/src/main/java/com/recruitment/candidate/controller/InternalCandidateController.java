package com.recruitment.candidate.controller;

import com.recruitment.candidate.dto.response.InternalCvResponse;
import com.recruitment.candidate.service.CandidateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Internal API consumed by the ATS Service to verify a candidate's CV. */
@RestController
@RequestMapping("/internal/cvs")
@RequiredArgsConstructor
@Tag(name = "Internal - Candidate")
public class InternalCandidateController {

    private final CandidateService candidateService;

    @GetMapping("/{cvId}")
    @Operation(summary = "Verify a CV and get a temporary download link (used by ATS Service)")
    public InternalCvResponse getCv(@PathVariable Long cvId) {
        return candidateService.getCvForInternalVerification(cvId);
    }
}
