package com.rms.iam.controller;

import com.rms.iam.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping
@Tag(name = "Internal - Users")
public class InternalUserController {

    private final UserService userService;
    private final com.rms.iam.repository.UserRepository userRepository;

    public InternalUserController(UserService userService, com.rms.iam.repository.UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @PostMapping({"/internal/users/{id}/deactivate", "/api/v1/internal/users/{id}/deactivate"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Internal user deactivation and session revocation")
    public void deactivateUser(@PathVariable UUID id) {
        userService.deactivateUserById(id);
    }

    @GetMapping({"/internal/tenants/{tenantId}/recruiters", "/api/v1/internal/tenants/{tenantId}/recruiters"})
    @Operation(summary = "Internal query of active recruiter / admin user IDs for a tenant")
    public java.util.List<UUID> getTenantRecruiters(@PathVariable UUID tenantId) {
        return userRepository.findByTenantIdAndStatus(tenantId, com.rms.iam.entity.UserStatus.ACTIVE).stream()
                .filter(u -> u.getRole() == com.rms.iam.entity.Role.RECRUITER || u.getRole() == com.rms.iam.entity.Role.HR_ADMIN)
                .map(com.rms.iam.entity.User::getId)
                .toList();
    }

    @GetMapping({"/internal/users/{userId}/identity", "/api/v1/internal/users/{userId}/identity"})
    @Operation(summary = "Internal lookup of a user's email and role")
    public InternalUserIdentity getUserIdentity(@PathVariable UUID userId) {
        return userRepository.findById(userId)
                .map(user -> new InternalUserIdentity(user.getId(), user.getEmail(), user.getRole().name(), user.getFullName()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User was not found"));
    }

    @GetMapping({"/internal/tenants/{tenantId}/users/{userId}", "/api/v1/internal/tenants/{tenantId}/users/{userId}"})
    @Operation(summary = "Internal lookup of an active tenant user")
    public InternalUserDetails getTenantUser(@PathVariable UUID tenantId, @PathVariable UUID userId) {
        return userRepository.findActiveByIdAndTenant(
                        userId, tenantId, com.rms.iam.entity.UserStatus.ACTIVE)
                .map(user -> new InternalUserDetails(user.getId(), tenantId, user.getRole().name(), user.getFullName(), user.getEmail()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Active tenant user was not found"));
    }

    @GetMapping({"/internal/tenants/{tenantId}/users/by-email", "/api/v1/internal/tenants/{tenantId}/users/by-email"})
    @Operation(summary = "Internal lookup of an active tenant user by email")
    public InternalUserDetails getTenantUserByEmail(
            @PathVariable UUID tenantId, @RequestParam String email) {
        return userRepository.findActiveByEmailAndTenant(
                        email.trim(), tenantId, com.rms.iam.entity.UserStatus.ACTIVE)
                .map(user -> new InternalUserDetails(user.getId(), tenantId, user.getRole().name(), user.getFullName(), user.getEmail()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Active tenant user was not found"));
    }

    public record InternalUserIdentity(UUID userId, String email, String role, String fullName) {
    }

    public record InternalUserDetails(UUID id, UUID tenantId, String role, String fullName, String email) {
    }
}
