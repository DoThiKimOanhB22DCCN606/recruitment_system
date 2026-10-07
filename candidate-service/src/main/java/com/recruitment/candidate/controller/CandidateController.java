package com.recruitment.candidate.controller;

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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/candidate")
@RequiredArgsConstructor
@Tag(name = "Candidate", description = "Candidate profile and CV management")
public class CandidateController {

    private final CandidateService candidateService;

    @GetMapping("/profile")
    @Operation(summary = "Get the authenticated candidate's profile")
    public ResponseEntity<CandidateProfileResponse> getProfile(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(candidateService.getProfile(userId(jwt)));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update personal info, education, experience, skills, certificates")
    public ResponseEntity<CandidateProfileResponse> updateProfile(
            @Valid @RequestBody CandidateProfileUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(candidateService.updateProfile(userId(jwt), request));
    }

    @PutMapping("/open-to-work")
    @Operation(summary = "Toggle 'Open To Work' status with an optional expiry window")
    public ResponseEntity<CandidateProfileResponse> setOpenToWork(
            @Valid @RequestBody OpenToWorkRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(candidateService.setOpenToWork(userId(jwt), request));
    }

    @GetMapping("/cvs")
    @Operation(summary = "List all CVs uploaded by the candidate")
    public ResponseEntity<List<CandidateCvResponse>> listCvs(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(candidateService.listCvs(userId(jwt)));
    }

    @PostMapping(value = "/cv", consumes = "multipart/form-data")
    @Operation(summary = "Upload a new CV file (stored in MinIO, bucket: candidate-cvs)")
    public ResponseEntity<CandidateCvResponse> uploadCv(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "setAsDefault", defaultValue = "false") boolean setAsDefault,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(candidateService.uploadCv(userId(jwt), file, setAsDefault));
    }

    @DeleteMapping("/cv/{id}")
    @Operation(summary = "Delete a CV")
    public ResponseEntity<Void> deleteCv(@PathVariable("id") Long cvId, @AuthenticationPrincipal Jwt jwt) {
        candidateService.deleteCv(userId(jwt), cvId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/cv/{id}/default")
    @Operation(summary = "Mark a CV as the default")
    public ResponseEntity<CandidateCvResponse> setDefaultCv(@PathVariable("id") Long cvId, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(candidateService.setDefaultCv(userId(jwt), cvId));
    }

    private Long userId(Jwt jwt) {
        if (jwt == null) return 101L; // demo: matches seed user_id in candidate table
        Object claim = jwt.getClaim("userId");
        return claim != null ? Long.valueOf(claim.toString()) : Long.valueOf(jwt.getSubject());
    }
}
