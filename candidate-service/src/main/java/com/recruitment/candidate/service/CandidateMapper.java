package com.recruitment.candidate.service;

import com.recruitment.candidate.dto.response.CandidateProfileResponse;
import com.recruitment.candidate.entity.*;
import org.springframework.stereotype.Component;

@Component
public class CandidateMapper {

    public CandidateProfileResponse toResponse(Candidate c) {
        return CandidateProfileResponse.builder()
                .id(c.getId())
                .userId(c.getUserId())
                .fullName(c.getFullName())
                .email(c.getEmail())
                .phone(c.getPhone())
                .dateOfBirth(c.getDateOfBirth())
                .gender(c.getGender())
                .address(c.getAddress())
                .headline(c.getHeadline())
                .summary(c.getSummary())
                .avatarUrl(c.getAvatarUrl())
                .openToWork(c.isOpenToWork())
                .openToWorkUntil(c.getOpenToWorkUntil())
                .educations(c.getEducations().stream().map(this::toEducationDto).toList())
                .experiences(c.getExperiences().stream().map(this::toExperienceDto).toList())
                .skills(c.getSkills().stream().map(this::toSkillDto).toList())
                .certificates(c.getCertificates().stream().map(this::toCertificateDto).toList())
                .languages(c.getLanguages().stream().map(this::toLanguageDto).toList())
                .portfolioLinks(c.getPortfolioLinks().stream().map(this::toPortfolioDto).toList())
                .build();
    }

    private CandidateProfileResponse.EducationDto toEducationDto(CandidateEducation e) {
        return CandidateProfileResponse.EducationDto.builder()
                .id(e.getId()).schoolName(e.getSchoolName()).degree(e.getDegree())
                .fieldOfStudy(e.getFieldOfStudy()).startDate(e.getStartDate()).endDate(e.getEndDate())
                .description(e.getDescription()).build();
    }

    private CandidateProfileResponse.ExperienceDto toExperienceDto(CandidateExperience e) {
        return CandidateProfileResponse.ExperienceDto.builder()
                .id(e.getId()).companyName(e.getCompanyName()).jobTitle(e.getJobTitle())
                .startDate(e.getStartDate()).endDate(e.getEndDate()).current(e.isCurrent())
                .description(e.getDescription()).build();
    }

    private CandidateProfileResponse.SkillDto toSkillDto(CandidateSkill s) {
        return CandidateProfileResponse.SkillDto.builder()
                .id(s.getId()).skillName(s.getSkillName()).proficiency(s.getProficiency()).build();
    }

    private CandidateProfileResponse.CertificateDto toCertificateDto(CandidateCertificate c) {
        return CandidateProfileResponse.CertificateDto.builder()
                .id(c.getId()).name(c.getName()).issuedBy(c.getIssuedBy())
                .issuedDate(c.getIssuedDate()).expiryDate(c.getExpiryDate())
                .credentialUrl(c.getCredentialUrl()).build();
    }

    private CandidateProfileResponse.LanguageDto toLanguageDto(CandidateLanguage l) {
        return CandidateProfileResponse.LanguageDto.builder()
                .id(l.getId()).language(l.getLanguage()).proficiency(l.getProficiency()).build();
    }

    private CandidateProfileResponse.PortfolioLinkDto toPortfolioDto(CandidatePortfolioLink p) {
        return CandidateProfileResponse.PortfolioLinkDto.builder()
                .id(p.getId()).title(p.getTitle()).url(p.getUrl()).build();
    }
}
