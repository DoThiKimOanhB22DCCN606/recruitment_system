package com.recruitment.candidate.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CandidateProfileUpdateRequest {

    @NotBlank
    private String fullName;

    @NotBlank @Email
    private String email;

    private String phone;
    private LocalDate dateOfBirth;
    private String gender;
    private String address;
    private String headline;
    private String summary;

    @Valid
    private List<EducationRequest> educations;
    @Valid
    private List<ExperienceRequest> experiences;
    @Valid
    private List<SkillRequest> skills;
    @Valid
    private List<CertificateRequest> certificates;
    @Valid
    private List<LanguageRequest> languages;
    @Valid
    private List<PortfolioLinkRequest> portfolioLinks;

    @Data
    public static class EducationRequest {
        @NotBlank
        private String schoolName;
        private String degree;
        private String fieldOfStudy;
        private LocalDate startDate;
        private LocalDate endDate;
        private String description;
    }

    @Data
    public static class ExperienceRequest {
        @NotBlank
        private String companyName;
        @NotBlank
        private String jobTitle;
        private LocalDate startDate;
        private LocalDate endDate;
        private boolean current;
        private String description;
    }

    @Data
    public static class SkillRequest {
        @NotBlank
        private String skillName;
        private String proficiency;
    }

    @Data
    public static class CertificateRequest {
        @NotBlank
        private String name;
        private String issuedBy;
        private LocalDate issuedDate;
        private LocalDate expiryDate;
        private String credentialUrl;
    }

    @Data
    public static class LanguageRequest {
        @NotBlank
        private String language;
        private String proficiency;
    }

    @Data
    public static class PortfolioLinkRequest {
        private String title;
        @NotBlank
        private String url;
    }
}
