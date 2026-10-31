package com.rms.iam.service;

import com.rms.iam.entity.Role;
import com.rms.iam.entity.Tenant;
import com.rms.iam.entity.User;
import com.rms.iam.entity.UserStatus;
import com.rms.iam.messaging.IamEventPublisher;
import com.rms.iam.repository.TenantRepository;
import com.rms.iam.repository.CompanyApprovalEmailRepository;
import com.rms.iam.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CompanyRegistrationServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final TenantRepository tenants = mock(TenantRepository.class);
    private final IamEventPublisher events = mock(IamEventPublisher.class);
    private final CompanyApprovalEmailRepository approvalEmails = mock(CompanyApprovalEmailRepository.class);
    private final CompanyRegistrationService service = new CompanyRegistrationService(users, tenants, events, approvalEmails);

    @Test
    void approvalCreatesTenantAndActivatesVerifiedHrAdmin() {
        UUID id = UUID.randomUUID();
        User user = pendingUser(id);
        when(users.findByIdForUpdate(id)).thenReturn(Optional.of(user));
        when(tenants.save(any(Tenant.class))).thenAnswer(invocation -> {
            Tenant tenant = invocation.getArgument(0);
            tenant.setId(UUID.randomUUID());
            return tenant;
        });

        Tenant tenant = service.approve(id);

        assertEquals("Example Co", tenant.getName());
        assertEquals("contact@example.com", tenant.getContactEmail());
        assertEquals("123456789", tenant.getTaxCode());
        assertSame(tenant, user.getTenant());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertNull(user.getPendingCompanyName());
        verify(approvalEmails).save(argThat(email -> email.getUserId().equals(id)
                && email.getRecipientEmail().equals(user.getEmail())
                && email.getCompanyName().equals("Example Co")));
        verify(events).publishUserCreatedEvent(id, user.getEmail(), "HR_ADMIN", tenant.getId(), user.getFullName());
    }

    @Test
    void rejectionDoesNotCreateTenantOrActivateAccount() {
        UUID id = UUID.randomUUID();
        User user = pendingUser(id);
        when(users.findByIdForUpdate(id)).thenReturn(Optional.of(user));

        service.reject(id);

        assertEquals(UserStatus.REJECTED, user.getStatus());
        assertNull(user.getTenant());
        verifyNoInteractions(tenants, events, approvalEmails);
    }

    private User pendingUser(UUID id) {
        User user = new User();
        user.setId(id);
        user.setEmail("hr@example.com");
        user.setFullName("HR User");
        user.setRole(Role.HR_ADMIN);
        user.setStatus(UserStatus.PENDING_APPROVAL);
        user.setPendingCompanyName("Example Co");
        user.setPendingContactEmail("contact@example.com");
        user.setPendingTaxCode("123456789");
        return user;
    }
}
