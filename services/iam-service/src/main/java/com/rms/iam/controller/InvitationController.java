package com.rms.iam.controller;

import com.rms.iam.dto.request.AcceptInvitationRequest;
import com.rms.iam.entity.Invitation;
import com.rms.iam.service.InvitationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/invitations")
public class InvitationController {

    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @GetMapping("/accept")
    public Map<String, String> validateInvitation(@RequestParam String token) {
        Invitation invitation = invitationService.validateToken(token);
        Map<String, String> response = new HashMap<>();
        response.put("email", invitation.getInvitedEmail());
        response.put("role", invitation.getRole().name());
        response.put("tenantId", invitation.getTenant().getId().toString());
        return response;
    }

    @PostMapping("/accept")
    @ResponseStatus(HttpStatus.CREATED)
    public void acceptInvitation(@Valid @RequestBody AcceptInvitationRequest request) {
        invitationService.acceptInvitation(request);
    }
}
