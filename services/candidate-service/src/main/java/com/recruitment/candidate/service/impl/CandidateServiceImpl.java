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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CandidateServiceImpl implements CandidateService {

    private final CandidateRepository candidateRepository;
    private final CandidateCvRepository cvRepository;
    private final com.recruitment.candidate.repository.SkillRepository skillRepository;
    private final MinioStorageService minioStorageService;
    private final CandidateMapper mapper;
    private final com.recruitment.candidate.client.AtsServiceClient atsServiceClient;
    private final com.recruitment.candidate.client.IamServiceClient iamServiceClient;
    private final com.recruitment.candidate.messaging.NotificationPublisher notificationPublisher;

    @Override
    public CandidateProfileResponse getProfile(UUID userUuid) {
        return mapper.toResponse(getOrCreateCandidate(userUuid));
    }

    @Override
    public CandidateProfileResponse getProfile(Long userId) {
        return mapper.toResponse(getOrCreateCandidate(userId));
    }

    @Override
    public CandidateProfileResponse updateProfile(UUID userUuid, CandidateProfileUpdateRequest request) {
        Candidate candidate = getOrCreateCandidate(userUuid);
        return updateProfileInternal(candidate, request);
    }

    @Override
    public CandidateProfileResponse updateProfile(Long userId, CandidateProfileUpdateRequest request) {
        Candidate candidate = getOrCreateCandidate(userId);
        return updateProfileInternal(candidate, request);
    }

    private CandidateProfileResponse updateProfileInternal(Candidate candidate, CandidateProfileUpdateRequest request) {
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
    public CandidateProfileResponse setOpenToWork(UUID userUuid, OpenToWorkRequest request) {
        Candidate candidate = getOrCreateCandidate(userUuid);
        return setOpenToWorkInternal(candidate, request);
    }

    @Override
    public CandidateProfileResponse setOpenToWork(Long userId, OpenToWorkRequest request) {
        Candidate candidate = getOrCreateCandidate(userId);
        return setOpenToWorkInternal(candidate, request);
    }

    private CandidateProfileResponse setOpenToWorkInternal(Candidate candidate, OpenToWorkRequest request) {
        candidate.setOpenToWork(Boolean.TRUE.equals(request.getOpenToWork()));
        candidate.setWarningSentAt(null);

        if (candidate.isOpenToWork()) {
            int days = request.getDurationDays() != null ? request.getDurationDays() : 30;
            candidate.setOpenToWorkUntil(LocalDateTime.now().plusDays(days));
        } else {
            candidate.setOpenToWorkUntil(null);
        }

        return mapper.toResponse(candidateRepository.save(candidate));
    }

    @Override
    public List<CandidateCvResponse> listCvs(UUID userUuid) {
        Candidate candidate = getOrCreateCandidate(userUuid);
        return listCvsInternal(candidate);
    }

    @Override
    public List<CandidateCvResponse> listCvs(Long userId) {
        Candidate candidate = getOrCreateCandidate(userId);
        return listCvsInternal(candidate);
    }

    private List<CandidateCvResponse> listCvsInternal(Candidate candidate) {
        return cvRepository.findByCandidateId(candidate.getId()).stream()
                .map(this::toCvResponse)
                .toList();
    }

    @Override
    public CandidateCvResponse uploadCv(UUID userUuid, MultipartFile file, boolean setAsDefault) {
        Candidate candidate = getOrCreateCandidate(userUuid);
        return uploadCvInternal(candidate, file, setAsDefault);
    }

    @Override
    public CandidateCvResponse uploadCv(Long userId, MultipartFile file, boolean setAsDefault) {
        Candidate candidate = getOrCreateCandidate(userId);
        return uploadCvInternal(candidate, file, setAsDefault);
    }

    private CandidateCvResponse uploadCvInternal(Candidate candidate, MultipartFile file, boolean setAsDefault) {
        if (file == null || file.isEmpty() || file.getSize() > 5_242_880) {
            throw new com.rms.common.exception.RmsException(
                    com.rms.common.exception.ErrorCode.ERR_FILE_TOO_LARGE,
                    "File size exceeds 5MB limit");
        }

        try (var is = file.getInputStream()) {
            byte[] header = new byte[4];
            int read = is.read(header);
            if (read < 4 || header[0] != 0x25 || header[1] != 0x50 || header[2] != 0x44 || header[3] != 0x46) {
                throw new com.rms.common.exception.RmsException(
                        com.rms.common.exception.ErrorCode.ERR_INVALID_FILE_TYPE,
                        "Invalid file format: only valid PDF files are permitted");
            }
        } catch (com.rms.common.exception.RmsException e) {
            throw e;
        } catch (Exception e) {
            throw new com.rms.common.exception.RmsException(
                    com.rms.common.exception.ErrorCode.ERR_INVALID_FILE_TYPE,
                    "Could not read uploaded file content");
        }

        long currentCount = cvRepository.countByCandidateId(candidate.getId());
        if (currentCount >= 3) {
            throw new com.rms.common.exception.RmsException(
                    com.rms.common.exception.ErrorCode.ERR_CV_LIMIT_EXCEEDED,
                    "Maximum limit of 3 CVs reached");
        }

        if (setAsDefault) {
            cvRepository.findByCandidateIdAndIsDefaultTrue(candidate.getId())
                    .ifPresent(existing -> { existing.setDefault(false); cvRepository.save(existing); });
        }

        String objectKey = minioStorageService.uploadCv(candidate.getId(), file);

        boolean isDefault = setAsDefault || currentCount == 0;
        CandidateCv cv = CandidateCv.builder()
                .candidate(candidate)
                .fileName(file.getOriginalFilename())
                .objectKey(objectKey)
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .isDefault(isDefault)
                .build();

        return toCvResponse(cvRepository.save(cv));
    }

    @Override
    public void deleteCv(UUID userUuid, Long cvId) {
        Candidate candidate = getOrCreateCandidate(userUuid);
        deleteCvInternal(candidate, cvId);
    }

    @Override
    public void deleteCv(Long userId, Long cvId) {
        Candidate candidate = getOrCreateCandidate(userId);
        deleteCvInternal(candidate, cvId);
    }

    private void deleteCvInternal(Candidate candidate, Long cvId) {
        CandidateCv cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new ResourceNotFoundException("CV not found: " + cvId));
        ensureOwnership(candidate, cv);

        try {
            boolean inUse = atsServiceClient.isCvInUse(cvId);
            if (inUse) {
                throw new com.rms.common.exception.RmsException(
                        com.rms.common.exception.ErrorCode.ERR_CV_IN_USE,
                        "Cannot delete CV: currently attached to active job applications");
            }
        } catch (com.rms.common.exception.RmsException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Unable to verify CV usage with ATS service (failing closed): {}", e.getMessage());
            throw new com.rms.common.exception.RmsException(
                    com.rms.common.exception.ErrorCode.ERR_SERVICE_UNAVAILABLE,
                    "Unable to verify if CV is in use by ATS service. Deletion rejected for safety.");
        }

        minioStorageService.deleteObject(cv.getObjectKey());
        cvRepository.delete(cv);

        if (cv.isDefault()) {
            List<CandidateCv> remaining = cvRepository.findByCandidateId(candidate.getId());
            if (!remaining.isEmpty()) {
                CandidateCv nextDefault = remaining.get(0);
                nextDefault.setDefault(true);
                cvRepository.save(nextDefault);
            }
        }
    }

    @Override
    public CandidateCvResponse setDefaultCv(UUID userUuid, Long cvId) {
        Candidate candidate = getOrCreateCandidate(userUuid);
        return setDefaultCvInternal(candidate, cvId);
    }

    @Override
    public CandidateCvResponse setDefaultCv(Long userId, Long cvId) {
        Candidate candidate = getOrCreateCandidate(userId);
        return setDefaultCvInternal(candidate, cvId);
    }

    private CandidateCvResponse setDefaultCvInternal(Candidate candidate, Long cvId) {
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
    public int notifyExpiringOpenToWorkStatuses() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now;
        LocalDateTime windowEnd = now.plusDays(3).plusHours(1);
        List<Candidate> expiringSoon = candidateRepository.findByOpenToWorkTrueAndOpenToWorkUntilBetweenAndWarningSentAtIsNull(windowStart, windowEnd);
        for (Candidate c : expiringSoon) {
            c.setWarningSentAt(now);
            notificationPublisher.publish(new com.recruitment.candidate.messaging.NotificationEvent(
                    null,
                    c.getUserUuid(),
                    "OPEN_TO_WORK_EXPIRING_SOON",
                    "CANDIDATE",
                    c.getUserUuid(),
                    "Open-To-Work Status Expiring Soon",
                    "Your 'Open To Work' status will expire in 3 days. Extend it in your profile to remain visible to employers."
            ));
        }
        candidateRepository.saveAll(expiringSoon);
        return expiringSoon.size();
    }

    @Override
    public int expireOpenToWorkStatuses() {
        List<Candidate> expired = candidateRepository
                .findByOpenToWorkTrueAndOpenToWorkUntilBefore(LocalDateTime.now());
        expired.forEach(c -> {
            c.setOpenToWork(false);
            c.setOpenToWorkUntil(null);
            c.setWarningSentAt(null);
            notificationPublisher.publish(new com.recruitment.candidate.messaging.NotificationEvent(
                    null,
                    c.getUserUuid(),
                    "OPEN_TO_WORK_EXPIRED",
                    "CANDIDATE",
                    c.getUserUuid(),
                    "Open-To-Work Status Expired",
                    "Your 'Open To Work' status has expired and was reverted to inactive."
            ));
        });
        candidateRepository.saveAll(expired);
        return expired.size();
    }

    @Override
    @Transactional(readOnly = true)
    public com.recruitment.candidate.dto.response.CandidateExportResponse exportData(UUID userUuid) {
        Candidate candidate = getOrCreateCandidate(userUuid);
        CandidateProfileResponse profile = mapper.toResponse(candidate);
        List<CandidateCvResponse> cvs = listCvsInternal(candidate);
        return com.recruitment.candidate.dto.response.CandidateExportResponse.builder()
                .profile(profile)
                .cvs(cvs)
                .exportedAt(java.time.Instant.now())
                .build();
    }

    @Override
    public void deleteAccount(UUID userUuid) {
        Candidate candidate = candidateRepository.findByUserUuid(userUuid).orElse(null);
        if (candidate != null) {
            log.info("Deactivating candidate account for ID: {} (UUID: {})", candidate.getId(), userUuid);
            candidate.setOpenToWork(false);
            candidate.setOpenToWorkUntil(null);
            candidateRepository.save(candidate);
        } else {
            log.info("Candidate not found for deactivation with UUID: {}", userUuid);
        }

        // Revoke IAM access & deactivate user
        iamServiceClient.deactivateUser(userUuid);
    }

    @Override
    public void anonymizeCandidate(UUID userUuid) {
        Candidate candidate = candidateRepository.findByUserUuid(userUuid).orElse(null);
        if (candidate == null) {
            log.info("Candidate not found for anonymization with UUID: {}", userUuid);
            return;
        }

        log.info("Executing candidate erasure and anonymization for ID: {} (UUID: {})", candidate.getId(), userUuid);

        // Delete CV files from MinIO & CV records
        List<CandidateCv> cvList = cvRepository.findByCandidateId(candidate.getId());
        for (CandidateCv cv : cvList) {
            try {
                minioStorageService.deleteObject(cv.getObjectKey());
            } catch (Exception e) {
                log.warn("Failed to delete CV file {} from MinIO: {}", cv.getObjectKey(), e.getMessage());
            }
        }
        cvRepository.deleteAll(cvList);

        // Anonymize PII
        candidate.setFullName("Anonymized Candidate");
        candidate.setEmail("anonymized_" + candidate.getId() + "@deleted.local");
        candidate.setPhone(null);
        candidate.setDateOfBirth(null);
        candidate.setGender(null);
        candidate.setAddress(null);
        candidate.setHeadline(null);
        candidate.setSummary(null);
        candidate.setAvatarUrl(null);
        candidate.setOpenToWork(false);
        candidate.setOpenToWorkUntil(null);

        // Clear sub-records
        candidate.getEducations().clear();
        candidate.getExperiences().clear();
        candidate.getSkills().clear();
        candidate.getCertificates().clear();
        candidate.getLanguages().clear();
        candidate.getPortfolioLinks().clear();

        candidateRepository.save(candidate);
    }

    // ---- helpers ----

    private Candidate getOrCreateCandidate(UUID userUuid) {
        return candidateRepository.findByUserUuid(userUuid)
                .orElseGet(() -> candidateRepository.save(Candidate.builder()
                        .userUuid(userUuid)
                        .userId(Math.abs(userUuid.getMostSignificantBits()))
                        .fullName("New Candidate")
                        .email("user-" + userUuid + "@placeholder.local")
                        .build()));
    }

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
        if (items != null) {
            items.forEach(dto -> candidate.getEducations().add(CandidateEducation.builder()
                    .candidate(candidate)
                    .schoolName(dto.getSchoolName())
                    .degree(dto.getDegree())
                    .fieldOfStudy(dto.getFieldOfStudy())
                    .startDate(dto.getStartDate())
                    .endDate(dto.getEndDate())
                    .description(dto.getDescription())
                    .build()));
        }
    }

    private void replaceExperiences(Candidate candidate, List<CandidateProfileUpdateRequest.ExperienceRequest> items) {
        candidate.getExperiences().clear();
        if (items != null) {
            items.forEach(dto -> candidate.getExperiences().add(CandidateExperience.builder()
                    .candidate(candidate)
                    .companyName(dto.getCompanyName())
                    .jobTitle(dto.getJobTitle())
                    .startDate(dto.getStartDate())
                    .endDate(dto.getEndDate())
                    .current(dto.isCurrent())
                    .description(dto.getDescription())
                    .build()));
        }
    }

    private void replaceSkills(Candidate candidate, List<CandidateProfileUpdateRequest.SkillRequest> items) {
        candidate.getSkills().clear();
        if (items != null) {
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (var dto : items) {
                if (dto.getSkillName() != null && !dto.getSkillName().trim().isEmpty()) {
                    String trimmed = dto.getSkillName().trim();
                    var skillOpt = skillRepository.findByNameIgnoreCase(trimmed);
                    if (skillOpt.isEmpty()) {
                        throw new org.springframework.web.server.ResponseStatusException(
                                org.springframework.http.HttpStatus.BAD_REQUEST,
                                "Unknown skill: " + trimmed + ". Skills must be selected from the predefined catalog.");
                    }
                    String officialName = skillOpt.get().getName();
                    String key = officialName.toLowerCase();
                    if (seen.add(key)) {
                        candidate.getSkills().add(CandidateSkill.builder()
                                .candidate(candidate)
                                .skillName(officialName)
                                .proficiency(dto.getProficiency())
                                .build());
                    }
                }
            }
        }
    }

    private void replaceCertificates(Candidate candidate, List<CandidateProfileUpdateRequest.CertificateRequest> items) {
        candidate.getCertificates().clear();
        if (items != null) {
            items.forEach(dto -> candidate.getCertificates().add(CandidateCertificate.builder()
                    .candidate(candidate)
                    .name(dto.getName())
                    .issuedBy(dto.getIssuedBy())
                    .issuedDate(dto.getIssuedDate())
                    .expiryDate(dto.getExpiryDate())
                    .credentialUrl(dto.getCredentialUrl())
                    .build()));
        }
    }

    private void replaceLanguages(Candidate candidate, List<CandidateProfileUpdateRequest.LanguageRequest> items) {
        candidate.getLanguages().clear();
        if (items != null) {
            items.forEach(dto -> candidate.getLanguages().add(CandidateLanguage.builder()
                    .candidate(candidate)
                    .language(dto.getLanguage())
                    .proficiency(dto.getProficiency())
                    .build()));
        }
    }

    private void replacePortfolioLinks(Candidate candidate, List<CandidateProfileUpdateRequest.PortfolioLinkRequest> items) {
        candidate.getPortfolioLinks().clear();
        if (items != null) {
            items.forEach(dto -> candidate.getPortfolioLinks().add(CandidatePortfolioLink.builder()
                    .candidate(candidate)
                    .title(dto.getTitle())
                    .url(dto.getUrl())
                    .build()));
        }
    }
}
