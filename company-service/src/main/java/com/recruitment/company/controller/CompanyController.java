package com.recruitment.company.controller;

import com.recruitment.company.dto.request.CompanyProfileUpdateRequest;
import com.recruitment.company.dto.response.CompanyProfileResponse;
import com.recruitment.company.service.CompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/company")
@RequiredArgsConstructor
@Tag(name = "Company", description = "Company profile and logo management")
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping
    @Operation(summary = "Get the authenticated employer's company profile")
    public ResponseEntity<CompanyProfileResponse> getProfile(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(companyService.getProfile(userId(jwt)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Public: get a company profile by id")
    public ResponseEntity<CompanyProfileResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(companyService.getById(id));
    }

    @PutMapping
    @Operation(summary = "Update company profile info, locations, and social links")
    public ResponseEntity<CompanyProfileResponse> updateProfile(
            @Valid @RequestBody CompanyProfileUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(companyService.updateProfile(userId(jwt), request));
    }

    @PostMapping(value = "/logo", consumes = "multipart/form-data")
    @Operation(summary = "Upload or replace the company logo (stored in MinIO, bucket: company-logos)")
    public ResponseEntity<CompanyProfileResponse> uploadLogo(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(companyService.uploadLogo(userId(jwt), file));
    }

    @DeleteMapping("/logo")
    @Operation(summary = "Delete the company logo")
    public ResponseEntity<CompanyProfileResponse> deleteLogo(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(companyService.deleteLogo(userId(jwt)));
    }

    private Long userId(Jwt jwt) {
        if (jwt == null) return 1L; // demo: matches seed owner_user_id in companies table
        Object claim = jwt.getClaim("userId");
        return claim != null ? Long.valueOf(claim.toString()) : Long.valueOf(jwt.getSubject());
    }
}
