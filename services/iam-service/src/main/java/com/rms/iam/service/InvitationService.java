package com.rms.iam.service;

import com.rms.common.exception.ErrorCode;
import com.rms.common.exception.RmsException;
import com.rms.iam.dto.request.AcceptInvitationRequest;
import com.rms.iam.entity.Invitation;
import com.rms.iam.entity.Role;
import com.rms.iam.entity.Tenant;
import com.rms.iam.entity.User;
import com.rms.iam.entity.UserStatus;
import com.rms.iam.repository.InvitationRepository;
import com.rms.iam.repository.TenantRepository;
import com.rms.iam.repository.UserRepository;
import com.rms.iam.messaging.IamEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final IamEventPublisher iamEventPublisher;

    public InvitationService(InvitationRepository invitationRepository,
                             UserRepository userRepository,
                             TenantRepository tenantRepository,
                             PasswordEncoder passwordEncoder,
                             JwtTokenProvider jwtTokenProvider,
                             IamEventPublisher iamEventPublisher) {
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.iamEventPublisher = iamEventPublisher;
    }

    @Transactional
    public void inviteUser(String email, String roleStr, UUID tenantId, UUID inviterId) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RmsException(ErrorCode.ERR_EMAIL_ALREADY_EXISTS, "Email already in use");
        }

        Role role;
        try {
            role = Role.valueOf(roleStr);
        } catch (IllegalArgumentException e) {
            throw new RmsException(ErrorCode.ERR_VALIDATION_FAILED, "Invalid role for invitation");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RmsException(ErrorCode.ERR_VALIDATION_FAILED, "Tenant not found"));

        String rawToken = jwtTokenProvider.generateRandomToken();
        String tokenHash = jwtTokenProvider.hashToken(rawToken);

        User inviter = userRepository.findById(inviterId)
                .orElseThrow(() -> new RmsException(ErrorCode.ERR_VALIDATION_FAILED, "Inviter not found"));

        Invitation invitation = new Invitation();
        invitation.setInvitedEmail(email);
        invitation.setRole(role);
        invitation.setTenant(tenant);
        invitation.setInvitedBy(inviter);
        invitation.setTokenHash(tokenHash);
        invitation.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        invitation.setAccepted(false);

        invitationRepository.save(invitation);

        // Send email via RabbitMQ
        iamEventPublisher.sendVerificationEmail(email, rawToken); // Note: Should probably be a specific invite email
    }

    @Transactional(readOnly = true)
    public Invitation validateToken(String rawToken) {
        String tokenHash = jwtTokenProvider.hashToken(rawToken);
        return invitationRepository.findByTokenHash(tokenHash)
                .filter(inv -> !inv.isAccepted() && inv.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new RmsException(ErrorCode.ERR_VALIDATION_FAILED, "Invalid or expired invitation token"));
    }

    @Transactional
    public void acceptInvitation(AcceptInvitationRequest request) {
        Invitation invitation = validateToken(request.getToken());

        Tenant tenant = invitation.getTenant();

        User user = new User();
        user.setEmail(invitation.getInvitedEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(invitation.getRole());
        user.setTenant(tenant);
        user.setStatus(UserStatus.ACTIVE);
        user.setPrivacyAcceptedAt(Instant.now());

        user = userRepository.save(user);

        invitation.setAccepted(true);
        invitationRepository.save(invitation);

        iamEventPublisher.publishUserCreatedEvent(user.getId(), user.getEmail(), user.getRole().name(), tenant.getId());
    }
}
