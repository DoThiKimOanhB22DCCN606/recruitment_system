package com.rms.iam.controller;

import com.rms.iam.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.rms.iam.dto.request.InviteUserRequest;
import com.rms.iam.dto.response.UserDto;
import com.rms.iam.service.InvitationService;
import jakarta.validation.Valid;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final InvitationService invitationService;

    public UserController(UserService userService, InvitationService invitationService) {
        this.userService = userService;
        this.invitationService = invitationService;
    }

    @PostMapping("/invite")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('HR_ADMIN')")
    public void inviteUser(
            @Valid @RequestBody InviteUserRequest request,
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader("X-User-Id") UUID inviterId) {
        invitationService.inviteUser(request.getEmail(), request.getRole(), tenantId, inviterId);
    }

    @GetMapping("/me")
    public UserDto getMe(@RequestHeader("X-User-Id") UUID userId) {
        return userService.getMe(userId);
    }

    @PatchMapping("/{id}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('HR_ADMIN')")
    public void deactivateUser(
            @PathVariable UUID id,
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        userService.deactivateUser(id, tenantId);
    }
}
