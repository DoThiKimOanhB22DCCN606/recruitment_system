package com.recruitment.company.service;

import com.recruitment.company.dto.request.CompanyProfileUpdateRequest;
import com.recruitment.company.dto.response.CompanyProfileResponse;
import com.recruitment.company.dto.response.InternalCompanyResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface CompanyService {

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
