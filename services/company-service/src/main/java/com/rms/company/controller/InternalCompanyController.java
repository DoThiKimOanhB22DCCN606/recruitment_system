package com.rms.company.controller;

import com.rms.company.dto.response.InternalCompanyResponse;
import com.rms.company.service.CompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

/** Internal API allowing Job Service / ATS Service to resolve basic company info. */
@RestController
@RequestMapping("/internal/company")
@RequiredArgsConstructor
@Tag(name = "Internal - Company")
public class InternalCompanyController {

    private final CompanyService companyService;

    @GetMapping("/{id}")
    @Operation(summary = "Get basic company info (used by other services)")
    public InternalCompanyResponse getCompany(@PathVariable Long id) {
        return companyService.getInternalCompany(id);
    }

    @PostMapping("/tenant/{tenantId}/resolve")
    @Operation(summary = "Resolve or create the company shared by a tenant")
    public InternalCompanyResponse getCompanyByTenant(@PathVariable UUID tenantId,
                                                       @RequestParam UUID userId) {
        return companyService.getInternalCompanyByTenant(tenantId, userId);
    }
}
