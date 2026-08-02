package com.recruitment.candidate.service.impl;

import com.recruitment.candidate.dto.request.CandidateProfileUpdateRequest;
import com.recruitment.candidate.dto.request.OpenToWorkRequest;
import com.recruitment.candidate.dto.response.CandidateCvResponse;
import com.recruitment.candidate.dto.response.CandidateProfileResponse;
import com.recruitment.candidate.dto.response.InternalCvResponse;
import com.recruitment.candidate.entity.*;
import com.recruitment.candidate.exception.ResourceNotFoundException;
import com.recruitment.candidate.minio.MinioStorageService;
import com.recruitment.candidate.repository.CandidateCvRepository;
import com.recruitment.candidate.repository.CandidateRepository;
import com.recruitment.candidate.service.CandidateMapper;
import com.recruitment.candidate.service.CandidateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CandidateServiceImpl implements CandidateService {

    private final CandidateRepository candidateRepository;
    private final CandidateCvRepository cvRepository;
    private final MinioStorageService minioStorageService;
    private final CandidateMapper mapper;

    @Override
    public CandidateProfileResponse getProfile(Long userId) {
        return mapper.toResponse(getOrCreateCandidate(userId));
    }

    @Override
    public CandidateProfileResponse updateProfile(Long userId, CandidateProfileUpdateRequest request) {
        Candidate candidate = getOrCreateCandidate(userId);

        candidate.setFullName(request.getFullName());
        candidate.setEmail(request.getEmail());
        candidate.setPhone(request.getPhone());
        candidate.setDateOfBirth(request.getDateOfBirth());
        candidate.setGender(request.getGender());
        candidate.setAddress(request.getAddress());
        candidate.setHeadline(request.getHeadline());
        candidate.setSummary(request.getSummary());

        replaceEducations(candidate, request.getEducations());
        replaceExperiences(candidate, request.getExperiences());
        replaceSkills(candidate, request.getSkills());
        replaceCertificates(candidate, request.getCertificates());
        replaceLanguages(candidate, request.getLanguages());
        replacePortfolioLinks(candidate, request.getPortfolioLinks());

        return mapper.toResponse(candidateRepository.save(candidate));
    }

    @Override
    public CandidateProfileResponse setOpenToWork(Long userId, OpenToWorkRequest request) {
        Candidate candidate = getOrCreateCandidate(userId);
        candidate.setOpenToWork(Boolean.TRUE.equals(request.getOpenToWork()));

        if (candidate.isOpenToWork()) {
            int days = request.getDurationDays() != null ? request.getDurationDays() : 30;
            candidate.setOpenToWorkUntil(LocalDateTime.now().plusDays(days));
        } else {
            candidate.setOpenToWorkUntil(null);
        }

        return mapper.toResponse(candidateRepository.save(candidate));
    }

    @Override
    public List<CandidateCvResponse> listCvs(Long userId) {
        Candidate candidate = getOrCreateCandidate(userId);
        return cvRepository.findByCandidateId(candidate.getId()).stream()
                .map(this::toCvResponse)
                .toList();
    }

    @Override
    public CandidateCvResponse uploadCv(Long userId, MultipartFile file, boolean setAsDefault) {
        Candidate candidate = getOrCreateCandidate(userId);
        String objectKey = minioStorageService.uploadCv(candidate.getId(), file);

        boolean noExistingCvs = cvRepository.findByCandidateId(candidate.getId()).isEmpty();
        boolean makeDefault = setAsDefault || noExistingCvs;

        if (makeDefault) {
            cvRepository.findByCandidateIdAndIsDefaultTrue(candidate.getId())
                    .ifPresent(existing -> { existing.setDefault(false); cvRepository.save(existing); });
        }

        CandidateCv cv = CandidateCv.builder()
                .candidate(candidate)
                .fileName(file.getOriginalFilename())
                .objectKey(objectKey)
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .isDefault(makeDefault)
                .build();

        return toCvResponse(cvRepository.save(cv));
    }

    @Override
    public void deleteCv(Long userId, Long cvId) {
        Candidate candidate = getOrCreateCandidate(userId);
        CandidateCv cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new ResourceNotFoundException("CV not found: " + cvId));
        ensureOwnership(candidate, cv);

        minioStorageService.deleteObject(cv.getObjectKey());
        cvRepository.delete(cv);
    }

    @Override
    public CandidateCvResponse setDefaultCv(Long userId, Long cvId) {
        Candidate candidate = getOrCreateCandidate(userId);
        CandidateCv target = cvRepository.findById(cvId)
                .orElseThrow(() -> new ResourceNotFoundException("CV not found: " + cvId));
        ensureOwnership(candidate, target);

        cvRepository.findByCandidateIdAndIsDefaultTrue(candidate.getId())
                .ifPresent(existing -> { existing.setDefault(false); cvRepository.save(existing); });

        target.setDefault(true);
        return toCvResponse(cvRepository.save(target));
    }

    @Override
    @Transactional(readOnly = true)
    public InternalCvResponse getCvForInternalVerification(Long cvId) {
        return cvRepository.findById(cvId)
                .map(cv -> InternalCvResponse.builder()
                        .cvId(cv.getId())
                        .candidateId(cv.getCandidate().getId())
                        .fileName(cv.getFileName())
                        .downloadUrl(minioStorageService.generatePresignedDownloadUrl(cv.getObjectKey()))
                        .valid(true)
                        .build())
                .orElse(InternalCvResponse.builder().cvId(cvId).valid(false).build());
    }

    @Override
    public int expireOpenToWorkStatuses() {
        List<Candidate> expired = candidateRepository
                .findByOpenToWorkTrueAndOpenToWorkUntilBefore(LocalDateTime.now());
        expired.forEach(c -> {
            c.setOpenToWork(false);
            c.setOpenToWorkUntil(null);
        });
        candidateRepository.saveAll(expired);
        return expired.size();
    }

    // ---- helpers ----

    private Candidate getOrCreateCandidate(Long userId) {
        return candidateRepository.findByUserId(userId)
                .orElseGet(() -> candidateRepository.save(Candidate.builder()
                        .userId(userId)
                        .fullName("New Candidate")
                        .email("user" + userId + "@placeholder.local")
                        .build()));
    }

    private void ensureOwnership(Candidate candidate, CandidateCv cv) {
        if (!cv.getCandidate().getId().equals(candidate.getId())) {
            throw new ResourceNotFoundException("CV not found for this candidate");
        }
    }

    private CandidateCvResponse toCvResponse(CandidateCv cv) {
        return CandidateCvResponse.builder()
                .id(cv.getId())
                .fileName(cv.getFileName())
                .fileSize(cv.getFileSize())
                .contentType(cv.getContentType())
                .isDefault(cv.isDefault())
                .downloadUrl(minioStorageService.generatePresignedDownloadUrl(cv.getObjectKey()))
                .uploadedAt(cv.getUploadedAt())
                .build();
    }

    private void replaceEducations(Candidate candidate, List<CandidateProfileUpdateRequest.EducationRequest> items) {
        candidate.getEducations().clear();
        if (items == null) return;
        items.forEach(i -> candidate.getEducations().add(CandidateEducation.builder()
                .candidate(candidate).schoolName(i.getSchoolName()).degree(i.getDegree())
                .fieldOfStudy(i.getFieldOfStudy()).startDate(i.getStartDate()).endDate(i.getEndDate())
                .description(i.getDescription()).build()));
    }

    private void replaceExperiences(Candidate candidate, List<CandidateProfileUpdateRequest.ExperienceRequest> items) {
        candidate.getExperiences().clear();
        if (items == null) return;
        items.forEach(i -> candidate.getExperiences().add(CandidateExperience.builder()
                .candidate(candidate).companyName(i.getCompanyName()).jobTitle(i.getJobTitle())
                .startDate(i.getStartDate()).endDate(i.getEndDate()).current(i.isCurrent())
                .description(i.getDescription()).build()));
    }

    private void replaceSkills(Candidate candidate, List<CandidateProfileUpdateRequest.SkillRequest> items) {
        candidate.getSkills().clear();
        if (items == null) return;
        items.forEach(i -> candidate.getSkills().add(CandidateSkill.builder()
                .candidate(candidate).skillName(i.getSkillName()).proficiency(i.getProficiency()).build()));
    }

    private void replaceCertificates(Candidate candidate, List<CandidateProfileUpdateRequest.CertificateRequest> items) {
        candidate.getCertificates().clear();
        if (items == null) return;
        items.forEach(i -> candidate.getCertificates().add(CandidateCertificate.builder()
                .candidate(candidate).name(i.getName()).issuedBy(i.getIssuedBy())
                .issuedDate(i.getIssuedDate()).expiryDate(i.getExpiryDate())
                .credentialUrl(i.getCredentialUrl()).build()));
    }

    private void replaceLanguages(Candidate candidate, List<CandidateProfileUpdateRequest.LanguageRequest> items) {
        candidate.getLanguages().clear();
        if (items == null) return;
        items.forEach(i -> candidate.getLanguages().add(CandidateLanguage.builder()
                .candidate(candidate).language(i.getLanguage()).proficiency(i.getProficiency()).build()));
    }

    private void replacePortfolioLinks(Candidate candidate, List<CandidateProfileUpdateRequest.PortfolioLinkRequest> items) {
        candidate.getPortfolioLinks().clear();
        if (items == null) return;
        items.forEach(i -> candidate.getPortfolioLinks().add(CandidatePortfolioLink.builder()
                .candidate(candidate).title(i.getTitle()).url(i.getUrl()).build()));
    }
}
