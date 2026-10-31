package com.rms.company.service;

import com.rms.company.dto.request.CompanyProfileUpdateRequest;
import com.rms.company.dto.response.CompanyProfileResponse;
import com.rms.company.dto.response.CompanyBrandResponse;
import com.rms.company.dto.response.InternalCompanyResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface CompanyService {

    CompanyProfileResponse getProfile(UUID tenantId, UUID userId);
    CompanyProfileResponse updateProfile(UUID tenantId, UUID userId, CompanyProfileUpdateRequest request);
    CompanyProfileResponse uploadLogo(UUID tenantId, UUID userId, MultipartFile file);
    byte[] getLogo(UUID tenantId, UUID userId);
    CompanyBrandResponse getBrandByTenant(UUID tenantId);
    byte[] getPublicLogoByTenant(UUID tenantId);
    CompanyProfileResponse deleteLogo(UUID tenantId, UUID userId);
    InternalCompanyResponse getInternalCompanyByTenant(UUID tenantId, UUID userId);

    CompanyProfileResponse getProfile(UUID ownerUserUuid);
    CompanyProfileResponse getProfile(Long ownerUserId);

    CompanyProfileResponse getById(Long companyId);

    CompanyProfileResponse updateProfile(UUID ownerUserUuid, CompanyProfileUpdateRequest request);
    CompanyProfileResponse updateProfile(Long ownerUserId, CompanyProfileUpdateRequest request);

    CompanyProfileResponse uploadLogo(UUID ownerUserUuid, MultipartFile file);
    CompanyProfileResponse uploadLogo(Long ownerUserId, MultipartFile file);

    CompanyProfileResponse deleteLogo(UUID ownerUserUuid);
    CompanyProfileResponse deleteLogo(Long ownerUserId);

    InternalCompanyResponse getInternalCompany(Long companyId);
}
