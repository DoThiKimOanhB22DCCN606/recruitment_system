package com.recruitment.candidate.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateProfileResponse {
    private Long id;
    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private String gender;
    private String address;
    private String headline;
    private String summary;
    private String avatarUrl;
    private boolean openToWork;
    private LocalDateTime openToWorkUntil;
    private List<EducationDto> educations;
    private List<ExperienceDto> experiences;
    private List<SkillDto> skills;
    private List<CertificateDto> certificates;
    private List<LanguageDto> languages;
    private List<PortfolioLinkDto> portfolioLinks;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class EducationDto {
        private Long id;
        private String schoolName;
        private String degree;
        private String fieldOfStudy;
        private LocalDate startDate;
        private LocalDate endDate;
        private String description;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ExperienceDto {
        private Long id;
        private String companyName;
        private String jobTitle;
        private LocalDate startDate;
        private LocalDate endDate;
        private boolean current;
        private String description;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SkillDto {
        private Long id;
        private String skillName;
        private String proficiency;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CertificateDto {
        private Long id;
        private String name;
        private String issuedBy;
        private LocalDate issuedDate;
        private LocalDate expiryDate;
        private String credentialUrl;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LanguageDto {
        private Long id;
        private String language;
        private String proficiency;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PortfolioLinkDto {
        private Long id;
        private String title;
        private String url;
    }
}
