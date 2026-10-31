package com.rms.iam.controller;

import com.rms.iam.entity.Tenant;
import com.rms.iam.repository.TenantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.rms.common.exception.RmsException;
import com.rms.common.exception.ErrorCode;
import org.springframework.security.access.prepost.PreAuthorize;
import com.rms.iam.service.TenantService;
import com.rms.iam.service.UserService;
import com.rms.iam.dto.request.CreateHrAdminRequest;
import com.rms.iam.dto.response.UserDto;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAuthority('SYS_ADMIN')")
public class AdminController {

    private final TenantService tenantService;
    private final com.rms.iam.service.AuditLogService auditLogService;
    private final UserService userService;
    private final com.rms.iam.service.CompanyRegistrationService companyRegistrationService;

    public AdminController(TenantService tenantService,
                           com.rms.iam.service.AuditLogService auditLogService,
                           UserService userService,
                           com.rms.iam.service.CompanyRegistrationService companyRegistrationService) {
        this.tenantService = tenantService;
        this.auditLogService = auditLogService;
        this.userService = userService;
        this.companyRegistrationService = companyRegistrationService;
    }

    @GetMapping("/company-registrations")
    public List<com.rms.iam.service.CompanyRegistrationService.PendingRegistration> pendingCompanyRegistrations() {
        return companyRegistrationService.pending();
    }

    @PostMapping("/company-registrations/{id}/approve")
    public Tenant approveCompanyRegistration(@PathVariable UUID id) {
        return companyRegistrationService.approve(id);
    }

    @PostMapping("/company-registrations/{id}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejectCompanyRegistration(@PathVariable UUID id) {
        companyRegistrationService.reject(id);
    }

    @PostMapping("/users/hr-admins")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createHrAdmin(@Valid @RequestBody CreateHrAdminRequest request) {
        return userService.createHrAdmin(request.getEmail(), request.getPassword(), request.getTenantId());
    }

    @GetMapping("/audit-logs")
    public org.springframework.data.domain.Page<com.rms.iam.entity.AuditLog> getAuditLogs(
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) java.time.Instant from,
            @RequestParam(required = false) java.time.Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return auditLogService.searchLogs(tenantId, actorId, entityId, actionType, from, to,
                org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("createdAt").descending()));
    }

    @GetMapping("/audit-logs/export")
    public org.springframework.http.ResponseEntity<?> exportAuditLogs(
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) java.time.Instant from,
            @RequestParam(required = false) java.time.Instant to,
            @RequestParam(defaultValue = "json") String format) {
        List<com.rms.iam.entity.AuditLog> logs = auditLogService.exportLogs(tenantId, actorId, entityId, actionType, from, to);
        if ("csv".equalsIgnoreCase(format)) {
            String csv = auditLogService.toCsv(logs);
            return org.springframework.http.ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, "text/csv")
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit_logs.csv\"")
                    .body(csv);
        }
        return org.springframework.http.ResponseEntity.ok(logs);
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
    public Tenant createTenant(@Valid @RequestBody Tenant tenant) {
        return tenantService.createTenant(tenant);
    }

    @PutMapping("/tenants/{id}")
    public Tenant updateTenant(@PathVariable UUID id, @Valid @RequestBody Tenant tenant) {
        return tenantService.updateTenant(id, tenant);
    }

    @DeleteMapping("/tenants/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTenant(@PathVariable UUID id) {
        tenantService.deleteTenant(id);
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
