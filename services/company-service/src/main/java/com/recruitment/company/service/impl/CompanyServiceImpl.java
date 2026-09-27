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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final MinioStorageService minioStorageService;
    private final CompanyMapper mapper;

    @Override
    public CompanyProfileResponse getProfile(UUID ownerUserUuid) {
        return mapper.toResponse(getOrCreateCompany(ownerUserUuid));
    }

    @Override
    public CompanyProfileResponse getProfile(Long ownerUserId) {
        return mapper.toResponse(getOrCreateCompany(ownerUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyProfileResponse getById(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId));
        return mapper.toPublicResponse(company);
    }

    @Override
    public CompanyProfileResponse updateProfile(UUID ownerUserUuid, CompanyProfileUpdateRequest request) {
        Company company = getOrCreateCompany(ownerUserUuid);
        return updateProfileInternal(company, request);
    }

    @Override
    public CompanyProfileResponse updateProfile(Long ownerUserId, CompanyProfileUpdateRequest request) {
        Company company = getOrCreateCompany(ownerUserId);
        return updateProfileInternal(company, request);
    }

    private CompanyProfileResponse updateProfileInternal(Company company, CompanyProfileUpdateRequest request) {
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
    public CompanyProfileResponse uploadLogo(UUID ownerUserUuid, MultipartFile file) {
        Company company = getOrCreateCompany(ownerUserUuid);
        return uploadLogoInternal(company, file);
    }

    @Override
    public CompanyProfileResponse uploadLogo(Long ownerUserId, MultipartFile file) {
        Company company = getOrCreateCompany(ownerUserId);
        return uploadLogoInternal(company, file);
    }

    private CompanyProfileResponse uploadLogoInternal(Company company, MultipartFile file) {
        // 1. File size validation (<= 2MB: 2,097,152 bytes)
        if (file == null || file.isEmpty() || file.getSize() > 2_097_152) {
            throw new com.rms.common.exception.RmsException(
                    com.rms.common.exception.ErrorCode.ERR_FILE_TOO_LARGE,
                    "File size exceeds 2MB limit for company logos");
        }

        // 2. Binary magic-byte validation (JPEG: FF D8 FF, PNG: 89 50 4E 47)
        try (var is = file.getInputStream()) {
            byte[] header = new byte[4];
            int read = is.read(header);
            boolean isJpeg = read >= 3 && (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF;
            boolean isPng = read >= 4 && (header[0] & 0xFF) == 0x89 && (header[1] & 0xFF) == 0x50 && (header[2] & 0xFF) == 0x4E && (header[3] & 0xFF) == 0x47;
            if (!isJpeg && !isPng) {
                throw new com.rms.common.exception.RmsException(
                        com.rms.common.exception.ErrorCode.ERR_INVALID_FILE_TYPE,
                        "Invalid image format: only JPEG and PNG files are permitted");
            }
        } catch (com.rms.common.exception.RmsException e) {
            throw e;
        } catch (Exception e) {
            throw new com.rms.common.exception.RmsException(
                    com.rms.common.exception.ErrorCode.ERR_INVALID_FILE_TYPE,
                    "Could not read uploaded logo file content");
        }

        // Replace: remove old logo object if present, then upload the new one
        if (company.getLogoObjectKey() != null) {
            minioStorageService.deleteObject(company.getLogoObjectKey());
        }
        String objectKey = minioStorageService.uploadLogo(company.getId(), file);
        company.setLogoObjectKey(objectKey);

        return mapper.toResponse(companyRepository.save(company));
    }

    @Override
    public CompanyProfileResponse deleteLogo(UUID ownerUserUuid) {
        Company company = getOrCreateCompany(ownerUserUuid);
        return deleteLogoInternal(company);
    }

    @Override
    public CompanyProfileResponse deleteLogo(Long ownerUserId) {
        Company company = getOrCreateCompany(ownerUserId);
        return deleteLogoInternal(company);
    }

    private CompanyProfileResponse deleteLogoInternal(Company company) {
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

    private Company getOrCreateCompany(UUID ownerUserUuid) {
        return companyRepository.findByOwnerUserUuid(ownerUserUuid)
                .orElseGet(() -> companyRepository.save(Company.builder()
                        .ownerUserUuid(ownerUserUuid)
                        .ownerUserId(Math.abs(ownerUserUuid.getMostSignificantBits()))
                        .name("New Company")
                        .build()));
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
