package com.rms.candidate.controller;

import com.rms.candidate.dto.response.InternalCvResponse;
import com.rms.candidate.service.CandidateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Internal API consumed by the ATS Service to verify a candidate's CV. */
@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Internal - Candidate")
public class InternalCandidateController {

    private final CandidateService candidateService;

    @GetMapping({"/internal/cvs/{cvId}", "/api/v1/internal/cvs/{cvId}"})
    @Operation(summary = "Verify a CV and get a temporary download link (used by ATS Service)")
    public InternalCvResponse getCv(@PathVariable Long cvId) {
        return candidateService.getCvForInternalVerification(cvId);
    }

    @GetMapping({"/internal/cvs/{cvId}/file", "/api/v1/internal/cvs/{cvId}/file"})
    @Operation(summary = "Download a CV for an authorized internal service")
    public ResponseEntity<byte[]> downloadCv(@PathVariable Long cvId) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(candidateService.getCvFileForInternalDownload(cvId));
    }
}
