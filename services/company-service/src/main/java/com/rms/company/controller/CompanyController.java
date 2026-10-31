package com.rms.company.controller;

import com.rms.company.config.UserPrincipal;
import com.rms.company.dto.request.CompanyProfileUpdateRequest;
import com.rms.company.dto.response.CompanyProfileResponse;
import com.rms.company.dto.response.CompanyBrandResponse;
import com.rms.company.service.CompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
            @RequestHeader(value = "X-User-Id", required = false) UUID headerUserId,
            @RequestHeader(value = "X-Tenant-Id", required = false) UUID headerTenantId) {
        return ResponseEntity.ok(companyService.getProfile(
                resolveTenantId(principal, headerTenantId), resolveUserId(principal, headerUserId)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Public: get a company profile by id")
    public ResponseEntity<CompanyProfileResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(companyService.getById(id));
    }

    @GetMapping("/tenant/{tenantId}/branding")
    @Operation(summary = "Public company name and logo availability for a tenant")
    public CompanyBrandResponse getBrandByTenant(@PathVariable UUID tenantId) {
        return companyService.getBrandByTenant(tenantId);
    }

    @GetMapping("/tenant/{tenantId}/logo")
    @Operation(summary = "Public company logo for a tenant")
    public ResponseEntity<byte[]> getPublicLogoByTenant(@PathVariable UUID tenantId) {
        return logoResponse(companyService.getPublicLogoByTenant(tenantId));
    }

    @PutMapping({"/profile", "/me", ""})
    @Operation(summary = "Update company profile info, locations, and social links")
    @PreAuthorize("hasAuthority('HR_ADMIN') or hasAuthority('RECRUITER')")
    public ResponseEntity<CompanyProfileResponse> updateProfile(
            @Valid @RequestBody CompanyProfileUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-User-Id", required = false) UUID headerUserId,
            @RequestHeader(value = "X-Tenant-Id", required = false) UUID headerTenantId) {
        return ResponseEntity.ok(companyService.updateProfile(
                resolveTenantId(principal, headerTenantId), resolveUserId(principal, headerUserId), request));
    }

    @PostMapping(value = "/logo", consumes = "multipart/form-data")
    @Operation(summary = "Upload or replace the company logo (stored in MinIO, bucket: company-logos)")
    @PreAuthorize("hasAuthority('HR_ADMIN') or hasAuthority('RECRUITER')")
    public ResponseEntity<CompanyProfileResponse> uploadLogo(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-User-Id", required = false) UUID headerUserId,
            @RequestHeader(value = "X-Tenant-Id", required = false) UUID headerTenantId) {
        return ResponseEntity.ok(companyService.uploadLogo(
                resolveTenantId(principal, headerTenantId), resolveUserId(principal, headerUserId), file));
    }

    @GetMapping("/logo")
    @Operation(summary = "Download the authenticated employer's company logo")
    @PreAuthorize("hasAuthority('HR_ADMIN') or hasAuthority('RECRUITER')")
    public ResponseEntity<byte[]> getLogo(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-User-Id", required = false) UUID headerUserId,
            @RequestHeader(value = "X-Tenant-Id", required = false) UUID headerTenantId) {
        byte[] logo = companyService.getLogo(
                resolveTenantId(principal, headerTenantId), resolveUserId(principal, headerUserId));
        return logoResponse(logo);
    }

    private ResponseEntity<byte[]> logoResponse(byte[] logo) {
        MediaType type = logo.length >= 4 && (logo[0] & 0xFF) == 0x89
                && (logo[1] & 0xFF) == 0x50 && (logo[2] & 0xFF) == 0x4E && (logo[3] & 0xFF) == 0x47
                ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(type).body(logo);
    }

    @DeleteMapping("/logo")
    @Operation(summary = "Delete the company logo")
    @PreAuthorize("hasAuthority('HR_ADMIN') or hasAuthority('RECRUITER')")
    public ResponseEntity<CompanyProfileResponse> deleteLogo(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-User-Id", required = false) UUID headerUserId,
            @RequestHeader(value = "X-Tenant-Id", required = false) UUID headerTenantId) {
        return ResponseEntity.ok(companyService.deleteLogo(
                resolveTenantId(principal, headerTenantId), resolveUserId(principal, headerUserId)));
    }

    private UUID resolveUserId(UserPrincipal principal, UUID headerUserId) {
        UUID uid = principal != null ? principal.userId() : headerUserId;
        if (uid != null) {
            return uid;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User authentication required");
    }

    private UUID resolveTenantId(UserPrincipal principal, UUID headerTenantId) {
        UUID tenantId = principal != null ? principal.tenantId() : headerTenantId;
        if (tenantId != null) return tenantId;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tenant membership required");
    }
}
