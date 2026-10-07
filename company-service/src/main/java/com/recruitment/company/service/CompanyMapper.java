package com.recruitment.company.service;

import com.recruitment.company.dto.response.CompanyProfileResponse;
import com.recruitment.company.entity.Company;
import com.recruitment.company.entity.CompanyLocation;
import com.recruitment.company.entity.CompanySocialLink;
import com.recruitment.company.minio.MinioStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CompanyMapper {

    private final MinioStorageService minioStorageService;

    public CompanyProfileResponse toResponse(Company c) {
        return CompanyProfileResponse.builder()
                .id(c.getId())
                .ownerUserId(c.getOwnerUserId())
                .name(c.getName())
                .description(c.getDescription())
                .industry(c.getIndustry())
                .companySize(c.getCompanySize())
                .website(c.getWebsite())
                .email(c.getEmail())
                .phone(c.getPhone())
                .logoUrl(minioStorageService.generatePresignedDownloadUrl(c.getLogoObjectKey()))
                .taxCode(c.getTaxCode())
                .foundedYear(c.getFoundedYear())
                .verified(c.isVerified())
                .locations(c.getLocations().stream().map(this::toLocationDto).toList())
                .socialLinks(c.getSocialLinks().stream().map(this::toSocialDto).toList())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    private CompanyProfileResponse.LocationDto toLocationDto(CompanyLocation l) {
        return CompanyProfileResponse.LocationDto.builder()
                .id(l.getId()).address(l.getAddress()).city(l.getCity())
                .country(l.getCountry()).headquarters(l.isHeadquarters()).build();
    }

    private CompanyProfileResponse.SocialLinkDto toSocialDto(CompanySocialLink s) {
        return CompanyProfileResponse.SocialLinkDto.builder()
                .id(s.getId()).platform(s.getPlatform()).url(s.getUrl()).build();
    }
}
