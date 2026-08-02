package com.recruitment.company.service;

import com.recruitment.company.dto.request.CompanyProfileUpdateRequest;
import com.recruitment.company.dto.response.CompanyProfileResponse;
import com.recruitment.company.dto.response.InternalCompanyResponse;
import org.springframework.web.multipart.MultipartFile;

public interface CompanyService {

    CompanyProfileResponse getProfile(Long ownerUserId);

    CompanyProfileResponse getById(Long companyId);

    CompanyProfileResponse updateProfile(Long ownerUserId, CompanyProfileUpdateRequest request);

    CompanyProfileResponse uploadLogo(Long ownerUserId, MultipartFile file);

    CompanyProfileResponse deleteLogo(Long ownerUserId);

    InternalCompanyResponse getInternalCompany(Long companyId);
}
