package com.rms.iam.service;

import com.rms.common.exception.ErrorCode;
import com.rms.common.exception.RmsException;
import com.rms.iam.entity.Role;
import com.rms.iam.entity.CompanyApprovalEmail;
import com.rms.iam.entity.Tenant;
import com.rms.iam.entity.TenantStatus;
import com.rms.iam.entity.User;
import com.rms.iam.entity.UserStatus;
import com.rms.iam.messaging.IamEventPublisher;
import com.rms.iam.repository.TenantRepository;
import com.rms.iam.repository.CompanyApprovalEmailRepository;
import com.rms.iam.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CompanyRegistrationService {
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final IamEventPublisher eventPublisher;
    private final CompanyApprovalEmailRepository approvalEmails;

    public CompanyRegistrationService(UserRepository userRepository, TenantRepository tenantRepository,
                                      IamEventPublisher eventPublisher,
                                      CompanyApprovalEmailRepository approvalEmails) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.eventPublisher = eventPublisher;
        this.approvalEmails = approvalEmails;
    }

    public record PendingRegistration(UUID id, String email, String fullName, String companyName,
                                      String contactEmail, String taxCode, Instant createdAt) {
        static PendingRegistration from(User user) {
            return new PendingRegistration(user.getId(), user.getEmail(), user.getFullName(),
                    user.getPendingCompanyName(), user.getPendingContactEmail(),
                    user.getPendingTaxCode(), user.getCreatedAt());
        }
    }

    @Transactional(readOnly = true)
    public List<PendingRegistration> pending() {
        return userRepository.findByRoleAndStatusOrderByCreatedAtAsc(Role.HR_ADMIN, UserStatus.PENDING_APPROVAL)
                .stream().map(PendingRegistration::from).toList();
    }

    @Transactional
    public Tenant approve(UUID userId) {
        User user = pendingUser(userId);
        Tenant tenant = new Tenant();
        tenant.setName(user.getPendingCompanyName());
        tenant.setContactEmail(user.getPendingContactEmail());
        tenant.setTaxCode(user.getPendingTaxCode());
        tenant.setStatus(TenantStatus.ACTIVE);
        tenant = tenantRepository.save(tenant);

        user.setTenant(tenant);
        user.setStatus(UserStatus.ACTIVE);
        user.setPendingCompanyName(null);
        user.setPendingContactEmail(null);
        user.setPendingTaxCode(null);
        userRepository.save(user);
        CompanyApprovalEmail email = new CompanyApprovalEmail();
        email.setUserId(user.getId());
        email.setRecipientEmail(user.getEmail());
        email.setCompanyName(tenant.getName());
        approvalEmails.save(email);
        eventPublisher.publishUserCreatedEvent(user.getId(), user.getEmail(), user.getRole().name(),
                tenant.getId(), user.getFullName());
        return tenant;
    }

    @Transactional
    public void reject(UUID userId) {
        User user = pendingUser(userId);
        user.setStatus(UserStatus.REJECTED);
        userRepository.save(user);
    }

    private User pendingUser(UUID userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new RmsException(ErrorCode.ERR_VALIDATION_FAILED, "Registration not found"));
        if (user.getRole() != Role.HR_ADMIN || user.getStatus() != UserStatus.PENDING_APPROVAL) {
            throw new RmsException(ErrorCode.ERR_VALIDATION_FAILED, "Registration is not pending approval");
        }
        return user;
    }
}
