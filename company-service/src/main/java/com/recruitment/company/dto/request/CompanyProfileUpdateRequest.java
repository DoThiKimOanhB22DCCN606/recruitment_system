package com.recruitment.company.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class CompanyProfileUpdateRequest {

    @NotBlank
    private String name;

    private String description;
    private String industry;
    private String companySize;
    private String website;
    private String email;
    private String phone;
    private String taxCode;
    private Integer foundedYear;

    @Valid
    private List<LocationRequest> locations;
    @Valid
    private List<SocialLinkRequest> socialLinks;

    @Data
    public static class LocationRequest {
        @NotBlank
        private String address;
        private String city;
        private String country;
        private boolean headquarters;
    }

    @Data
    public static class SocialLinkRequest {
        @NotBlank
        private String platform;
        @NotBlank
        private String url;
    }
}
