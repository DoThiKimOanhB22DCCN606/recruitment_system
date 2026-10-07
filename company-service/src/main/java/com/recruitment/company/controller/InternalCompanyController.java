package com.recruitment.company.controller;

import com.recruitment.company.dto.response.InternalCompanyResponse;
import com.recruitment.company.service.CompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
