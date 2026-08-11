package com.rms.iam.service;

import com.rms.common.exception.ErrorCode;
import com.rms.common.exception.RmsException;
import com.rms.iam.entity.Tenant;
import com.rms.iam.entity.TenantStatus;
import com.rms.iam.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

@Service
public class TenantService {

    private static final Logger log = LoggerFactory.getLogger(TenantService.class);

    private final TenantRepository tenantRepository;
    private final TokenBlacklistService tokenBlacklistService;

    public TenantService(TenantRepository tenantRepository, TokenBlacklistService tokenBlacklistService) {
        this.tenantRepository = tenantRepository;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    public List<Tenant> getAllTenants() {
        return tenantRepository.findAll();
    }

    public Tenant getTenant(UUID id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new RmsException(ErrorCode.ERR_VALIDATION_FAILED, "Tenant not found"));
    }

    @Transactional
    public Tenant createTenant(Tenant tenant) {
        return tenantRepository.save(tenant);
    }

    @Transactional
    public void suspendTenant(UUID id, UUID actorId) {
        Tenant tenant = getTenant(id);
        tenant.setStatus(TenantStatus.SUSPENDED);
        tenantRepository.save(tenant);
        
        tokenBlacklistService.blacklistTenant(id, 900); // 15 mins
        
        log.info("audit_log: tenant_suspended - tenant_id={} actor_id={}", id, actorId);
    }

    @Transactional
    public void activateTenant(UUID id) {
        Tenant tenant = getTenant(id);
        tenant.setStatus(TenantStatus.ACTIVE);
        tenantRepository.save(tenant);
    }
}
