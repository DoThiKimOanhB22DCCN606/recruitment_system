package com.recruitment.company.controller;

import com.recruitment.company.config.UserPrincipal;
import com.recruitment.company.dto.request.CompanyProfileUpdateRequest;
import com.recruitment.company.dto.response.CompanyProfileResponse;
import com.recruitment.company.service.CompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/companies", "/api/v1/company", "/companies", "/company"})
@RequiredArgsConstructor
@Tag(name = "Company", description = "Company profile and logo management")
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping({"/profile", "/me", ""})
    @Operation(summary = "Get the authenticated employer's company profile")
    @PreAuthorize("hasAuthority('HR_ADMIN') or hasAuthority('RECRUITER')")
    public ResponseEntity<CompanyProfileResponse> getProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-User-Id", required = false) UUID headerUserId) {
        return ResponseEntity.ok(companyService.getProfile(resolveUserId(principal, headerUserId)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Public: get a company profile by id")
    public ResponseEntity<CompanyProfileResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(companyService.getById(id));
    }

    @PutMapping({"/profile", "/me", ""})
    @Operation(summary = "Update company profile info, locations, and social links")
    @PreAuthorize("hasAuthority('HR_ADMIN') or hasAuthority('RECRUITER')")
    public ResponseEntity<CompanyProfileResponse> updateProfile(
            @Valid @RequestBody CompanyProfileUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-User-Id", required = false) UUID headerUserId) {
        return ResponseEntity.ok(companyService.updateProfile(resolveUserId(principal, headerUserId), request));
    }

    @PostMapping(value = "/logo", consumes = "multipart/form-data")
    @Operation(summary = "Upload or replace the company logo (stored in MinIO, bucket: company-logos)")
    @PreAuthorize("hasAuthority('HR_ADMIN') or hasAuthority('RECRUITER')")
    public ResponseEntity<CompanyProfileResponse> uploadLogo(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-User-Id", required = false) UUID headerUserId) {
        return ResponseEntity.ok(companyService.uploadLogo(resolveUserId(principal, headerUserId), file));
    }

    @DeleteMapping("/logo")
    @Operation(summary = "Delete the company logo")
    @PreAuthorize("hasAuthority('HR_ADMIN') or hasAuthority('RECRUITER')")
    public ResponseEntity<CompanyProfileResponse> deleteLogo(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-User-Id", required = false) UUID headerUserId) {
        return ResponseEntity.ok(companyService.deleteLogo(resolveUserId(principal, headerUserId)));
    }

    private UUID resolveUserId(UserPrincipal principal, UUID headerUserId) {
        UUID uid = principal != null ? principal.userId() : headerUserId;
        if (uid != null) {
            return uid;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User authentication required");
    }
}
