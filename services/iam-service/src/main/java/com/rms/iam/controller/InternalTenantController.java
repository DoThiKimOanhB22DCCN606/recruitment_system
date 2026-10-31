package com.rms.iam.controller;

import com.rms.iam.entity.Tenant;
import com.rms.iam.service.TenantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/tenants")
public class InternalTenantController {
    private final TenantService tenantService;

    public InternalTenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping("/{id}")
    public TenantName getName(@PathVariable UUID id) {
        return new TenantName(tenantService.getTenant(id).getName());
    }

    @PutMapping("/{id}/name")
    public TenantName updateName(@PathVariable UUID id, @Valid @RequestBody TenantName request) {
        return new TenantName(tenantService.updateTenantName(id, request.name()).getName());
    }

    public record TenantName(@NotBlank @Size(max = 255) String name) {}
}
