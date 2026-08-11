package com.rms.iam.controller;

import com.rms.iam.entity.Tenant;
import com.rms.iam.repository.TenantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.rms.common.exception.RmsException;
import com.rms.common.exception.ErrorCode;
import org.springframework.security.access.prepost.PreAuthorize;
import com.rms.iam.service.TenantService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAuthority('SYS_ADMIN')")
public class AdminController {

    private final TenantService tenantService;

    public AdminController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping("/tenants")
    public List<Tenant> getAllTenants() {
        return tenantService.getAllTenants();
    }

    @GetMapping("/tenants/{id}")
    public Tenant getTenant(@PathVariable UUID id) {
        return tenantService.getTenant(id);
    }
    
    @PostMapping("/tenants")
    @ResponseStatus(HttpStatus.CREATED)
    public Tenant createTenant(@RequestBody Tenant tenant) {
        return tenantService.createTenant(tenant);
    }

    @PatchMapping("/tenants/{id}/suspend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void suspendTenant(@PathVariable UUID id, @RequestHeader("X-User-Id") UUID actorId) {
        tenantService.suspendTenant(id, actorId);
    }

    @PatchMapping("/tenants/{id}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activateTenant(@PathVariable UUID id) {
        tenantService.activateTenant(id);
    }
}
