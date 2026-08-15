package com.recruitment.company.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyProfileResponse {
    private Long id;
    private Long ownerUserId;
    private String name;
    private String description;
    private String industry;
    private String companySize;
    private String website;
    private String email;
    private String phone;
    private String logoUrl;
    private String taxCode;
    private Integer foundedYear;
    private boolean verified;
    private List<LocationDto> locations;
    private List<SocialLinkDto> socialLinks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LocationDto {
        private Long id;
        private String address;
        private String city;
        private String country;
        private boolean headquarters;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SocialLinkDto {
        private Long id;
        private String platform;
        private String url;
    }
}
