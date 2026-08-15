package com.recruitment.company.service.impl;

import com.recruitment.company.dto.request.CompanyProfileUpdateRequest;
import com.recruitment.company.dto.response.CompanyProfileResponse;
import com.recruitment.company.dto.response.InternalCompanyResponse;
import com.recruitment.company.entity.Company;
import com.recruitment.company.entity.CompanyLocation;
import com.recruitment.company.entity.CompanySocialLink;
import com.recruitment.company.exception.ResourceNotFoundException;
import com.recruitment.company.minio.MinioStorageService;
import com.recruitment.company.repository.CompanyRepository;
import com.recruitment.company.service.CompanyMapper;
import com.recruitment.company.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final MinioStorageService minioStorageService;
    private final CompanyMapper mapper;

    @Override
    public CompanyProfileResponse getProfile(Long ownerUserId) {
        return mapper.toResponse(getOrCreateCompany(ownerUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyProfileResponse getById(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId));
        return mapper.toResponse(company);
    }

    @Override
    public CompanyProfileResponse updateProfile(Long ownerUserId, CompanyProfileUpdateRequest request) {
        Company company = getOrCreateCompany(ownerUserId);

        company.setName(request.getName());
        company.setDescription(request.getDescription());
        company.setIndustry(request.getIndustry());
        company.setCompanySize(request.getCompanySize());
        company.setWebsite(request.getWebsite());
        company.setEmail(request.getEmail());
        company.setPhone(request.getPhone());
        company.setTaxCode(request.getTaxCode());
        company.setFoundedYear(request.getFoundedYear());

        replaceLocations(company, request.getLocations());
        replaceSocialLinks(company, request.getSocialLinks());

        return mapper.toResponse(companyRepository.save(company));
    }

    @Override
    public CompanyProfileResponse uploadLogo(Long ownerUserId, MultipartFile file) {
        Company company = getOrCreateCompany(ownerUserId);

        // Replace: remove old logo object if present, then upload the new one
        if (company.getLogoObjectKey() != null) {
            minioStorageService.deleteObject(company.getLogoObjectKey());
        }
        String objectKey = minioStorageService.uploadLogo(company.getId(), file);
        company.setLogoObjectKey(objectKey);

        return mapper.toResponse(companyRepository.save(company));
    }

    @Override
    public CompanyProfileResponse deleteLogo(Long ownerUserId) {
        Company company = getOrCreateCompany(ownerUserId);
        if (company.getLogoObjectKey() != null) {
            minioStorageService.deleteObject(company.getLogoObjectKey());
            company.setLogoObjectKey(null);
        }
        return mapper.toResponse(companyRepository.save(company));
    }

    @Override
    @Transactional(readOnly = true)
    public InternalCompanyResponse getInternalCompany(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId));
        return new InternalCompanyResponse(
                company.getId(),
                company.getName(),
                minioStorageService.generatePresignedDownloadUrl(company.getLogoObjectKey()),
                company.isVerified()
        );
    }

    private Company getOrCreateCompany(Long ownerUserId) {
        return companyRepository.findByOwnerUserId(ownerUserId)
                .orElseGet(() -> companyRepository.save(Company.builder()
                        .ownerUserId(ownerUserId)
                        .name("New Company")
                        .build()));
    }

    private void replaceLocations(Company company, List<CompanyProfileUpdateRequest.LocationRequest> items) {
        company.getLocations().clear();
        if (items == null) return;
        items.forEach(i -> company.getLocations().add(CompanyLocation.builder()
                .company(company).address(i.getAddress()).city(i.getCity())
                .country(i.getCountry()).headquarters(i.isHeadquarters()).build()));
    }

    private void replaceSocialLinks(Company company, List<CompanyProfileUpdateRequest.SocialLinkRequest> items) {
        company.getSocialLinks().clear();
        if (items == null) return;
        items.forEach(i -> company.getSocialLinks().add(CompanySocialLink.builder()
                .company(company).platform(i.getPlatform()).url(i.getUrl()).build()));
    }
}
