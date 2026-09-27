package com.recruitment.candidate.controller;

import com.recruitment.candidate.config.UserPrincipal;
import com.recruitment.candidate.dto.request.CandidateProfileUpdateRequest;
import com.recruitment.candidate.dto.request.OpenToWorkRequest;
import com.recruitment.candidate.dto.response.CandidateCvResponse;
import com.recruitment.candidate.dto.response.CandidateProfileResponse;
import com.recruitment.candidate.service.CandidateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/candidate", "/candidate", "/api/v1/profiles", "/api/v1"})
@RequiredArgsConstructor
@Tag(name = "Candidate", description = "Candidate profile and CV management")
public class CandidateController {

    private final CandidateService candidateService;

    @GetMapping("/profile")
    @Operation(summary = "Get the authenticated candidate's profile")
    public ResponseEntity<CandidateProfileResponse> getProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-User-Id", required = false) UUID headerUserId) {
        return ResponseEntity.ok(candidateService.getProfile(resolveUserId(principal, headerUserId)));
    }

    @GetMapping({"/profile/export", "/candidate/profile/export"})
    @Operation(summary = "Export all candidate personal data in JSON format (GDPR Right to Access)")
    public ResponseEntity<com.recruitment.candidate.dto.response.CandidateExportResponse> exportData(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-User-Id", required = false) UUID headerUserId) {
        return ResponseEntity.ok(candidateService.exportData(resolveUserId(principal, headerUserId)));
    }

    @DeleteMapping({"/account", "/candidate/account"})
    @Operation(summary = "Request account erasure and PII anonymization (GDPR Right to Erasure)")
    public ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-User-Id", required = false) UUID headerUserId) {
        candidateService.deleteAccount(resolveUserId(principal, headerUserId));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/profile")
    @Operation(summary = "Update personal info, education, experience, skills, certificates")
    public ResponseEntity<CandidateProfileResponse> updateProfile(
            @Valid @RequestBody CandidateProfileUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-User-Id", required = false) UUID headerUserId) {
        return ResponseEntity.ok(candidateService.updateProfile(resolveUserId(principal, headerUserId), request));
    }

    @PutMapping("/open-to-work")
    @Operation(summary = "Toggle 'Open To Work' status with an optional expiry window")
    public ResponseEntity<CandidateProfileResponse> setOpenToWork(
            @Valid @RequestBody OpenToWorkRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-User-Id", required = false) UUID headerUserId) {
        return ResponseEntity.ok(candidateService.setOpenToWork(resolveUserId(principal, headerUserId), request));
    }

    @GetMapping("/cvs")
    @Operation(summary = "List all CVs uploaded by the candidate")
    public ResponseEntity<List<CandidateCvResponse>> listCvs(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-User-Id", required = false) UUID headerUserId) {
        return ResponseEntity.ok(candidateService.listCvs(resolveUserId(principal, headerUserId)));
    }

    @PostMapping(value = "/cv", consumes = "multipart/form-data")
    @Operation(summary = "Upload a new CV file (stored in MinIO, bucket: candidate-cvs)")
    public ResponseEntity<CandidateCvResponse> uploadCv(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "setAsDefault", defaultValue = "false") boolean setAsDefault,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-User-Id", required = false) UUID headerUserId) {
        return ResponseEntity.ok(candidateService.uploadCv(resolveUserId(principal, headerUserId), file, setAsDefault));
    }

    @DeleteMapping("/cv/{id}")
    @Operation(summary = "Delete a CV")
    public ResponseEntity<Void> deleteCv(
            @PathVariable("id") Long cvId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-User-Id", required = false) UUID headerUserId) {
        candidateService.deleteCv(resolveUserId(principal, headerUserId), cvId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/cv/{id}/default")
    @Operation(summary = "Mark a CV as the default")
    public ResponseEntity<CandidateCvResponse> setDefaultCv(
            @PathVariable("id") Long cvId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-User-Id", required = false) UUID headerUserId) {
        return ResponseEntity.ok(candidateService.setDefaultCv(resolveUserId(principal, headerUserId), cvId));
    }

    private UUID resolveUserId(UserPrincipal principal, UUID headerUserId) {
        UUID uid = principal != null ? principal.userId() : headerUserId;
        if (uid != null) {
            return uid;
        }
        throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED, "User authentication required");
    }
}
