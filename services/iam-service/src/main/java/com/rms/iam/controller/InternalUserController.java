package com.rms.iam.controller;

import com.rms.iam.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
}
