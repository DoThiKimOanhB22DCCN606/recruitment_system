package com.rms.company.service.impl;

import com.rms.company.client.TenantClient;
import com.rms.company.dto.request.CompanyProfileUpdateRequest;
import com.rms.company.dto.response.CompanyProfileResponse;
import com.rms.company.dto.response.CompanyBrandResponse;
import com.rms.company.dto.response.InternalCompanyResponse;
import com.rms.company.entity.Company;
import com.rms.company.entity.CompanyLocation;
import com.rms.company.entity.CompanySocialLink;
import com.rms.company.exception.ResourceNotFoundException;
import com.rms.company.minio.MinioStorageService;
import com.rms.company.repository.CompanyRepository;
import com.rms.company.service.CompanyMapper;
import com.rms.company.service.CompanyService;
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
    private final TenantClient tenantClient;

    @Override
    public CompanyProfileResponse getProfile(UUID tenantId, UUID userId) {
        return mapper.toResponse(getOrCreateCompany(tenantId, userId));
    }

    @Override
    public CompanyProfileResponse updateProfile(UUID tenantId, UUID userId, CompanyProfileUpdateRequest request) {
        Company company = getOrCreateCompany(tenantId, userId);
        String name = request.getName().trim();
        if (!name.equals(company.getName())) {
            tenantClient.updateName(tenantId, name);
        }
        request.setName(name);
        return updateProfileInternal(company, request);
    }

    @Override
    public CompanyProfileResponse uploadLogo(UUID tenantId, UUID userId, MultipartFile file) {
        return uploadLogoInternal(getOrCreateCompany(tenantId, userId), file);
    }

    @Override
    public byte[] getLogo(UUID tenantId, UUID userId) {
        Company company = getOrCreateCompany(tenantId, userId);
        if (company.getLogoObjectKey() == null) {
            throw new ResourceNotFoundException("Company logo not found");
        }
        return minioStorageService.downloadLogo(company.getLogoObjectKey());
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyBrandResponse getBrandByTenant(UUID tenantId) {
        Company company = companyRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found for tenant"));
        String objectKey = company.getLogoObjectKey();
        return new CompanyBrandResponse(company.getName(), objectKey != null,
                objectKey == null ? null : Integer.toHexString(objectKey.hashCode()));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getPublicLogoByTenant(UUID tenantId) {
        Company company = companyRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found for tenant"));
        if (company.getLogoObjectKey() == null) {
            throw new ResourceNotFoundException("Company logo not found");
        }
        return minioStorageService.downloadLogo(company.getLogoObjectKey());
    }

    @Override
    public CompanyProfileResponse deleteLogo(UUID tenantId, UUID userId) {
        return deleteLogoInternal(getOrCreateCompany(tenantId, userId));
    }

    @Override
    public InternalCompanyResponse getInternalCompanyByTenant(UUID tenantId, UUID userId) {
        Company company = getOrCreateCompany(tenantId, userId);
        return getInternalCompany(company.getId());
    }

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

    private Company getOrCreateCompany(UUID tenantId, UUID userId) {
        if (tenantId == null || userId == null) {
            throw new com.rms.common.exception.RmsException(
                    com.rms.common.exception.ErrorCode.ERR_VALIDATION_FAILED,
                    "Tenant ID and user ID are required for a company profile");
        }

        String tenantName = tenantClient.getName(tenantId);
        Company company = companyRepository.findByTenantId(tenantId).orElseGet(() -> {
            Company legacy = companyRepository.findByOwnerUserUuid(userId).orElse(null);
            if (legacy != null && legacy.getTenantId() == null) {
                legacy.setTenantId(tenantId);
                legacy.setName(tenantName);
                return companyRepository.save(legacy);
            }
            return companyRepository.save(Company.builder()
                    .tenantId(tenantId)
                    .ownerUserUuid(userId)
                    .ownerUserId(Math.abs(userId.getMostSignificantBits()))
                    .name(tenantName)
                    .build());
        });
        if (!tenantName.equals(company.getName())) {
            company.setName(tenantName);
            return companyRepository.save(company);
        }
        return company;
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
