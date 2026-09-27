package com.rms.iam.service;

import com.rms.iam.entity.Tenant;
import com.rms.iam.entity.TenantStatus;
import com.rms.iam.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private TokenBlacklistService tokenBlacklistService;

    private TenantService tenantService;

    @BeforeEach
    void setUp() {
        tenantService = new TenantService(tenantRepository, tokenBlacklistService);
    }

    @Test
    void updateTenantSuccess() {
        UUID tenantId = UUID.randomUUID();
        Tenant existing = new Tenant("Old Name", TenantStatus.ACTIVE);
        existing.setId(tenantId);

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(existing));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        Tenant updateReq = new Tenant("New Name", TenantStatus.SUSPENDED);
        Tenant result = tenantService.updateTenant(tenantId, updateReq);

        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getStatus()).isEqualTo(TenantStatus.SUSPENDED);
        verify(tenantRepository).save(existing);
    }

    @Test
    void deleteTenantSuccess() {
        UUID tenantId = UUID.randomUUID();
        Tenant existing = new Tenant("Target Tenant", TenantStatus.ACTIVE);
        existing.setId(tenantId);

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(existing));

        tenantService.deleteTenant(tenantId);

        verify(tenantRepository).delete(existing);
        verify(tokenBlacklistService).blacklistTenant(tenantId, 900);
    }
}