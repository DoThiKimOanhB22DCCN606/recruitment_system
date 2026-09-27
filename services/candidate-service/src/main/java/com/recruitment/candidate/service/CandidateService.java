package com.recruitment.candidate.service;

import com.recruitment.candidate.dto.request.CandidateProfileUpdateRequest;
import com.recruitment.candidate.dto.request.OpenToWorkRequest;
import com.recruitment.candidate.dto.response.CandidateCvResponse;
import com.recruitment.candidate.dto.response.CandidateProfileResponse;
import com.recruitment.candidate.dto.response.InternalCvResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface CandidateService {

    CandidateProfileResponse getProfile(UUID userUuid);
    CandidateProfileResponse getProfile(Long userId);

    CandidateProfileResponse updateProfile(UUID userUuid, CandidateProfileUpdateRequest request);
    CandidateProfileResponse updateProfile(Long userId, CandidateProfileUpdateRequest request);

    CandidateProfileResponse setOpenToWork(UUID userUuid, OpenToWorkRequest request);
    CandidateProfileResponse setOpenToWork(Long userId, OpenToWorkRequest request);

    List<CandidateCvResponse> listCvs(UUID userUuid);
    List<CandidateCvResponse> listCvs(Long userId);

    CandidateCvResponse uploadCv(UUID userUuid, MultipartFile file, boolean setAsDefault);
    CandidateCvResponse uploadCv(Long userId, MultipartFile file, boolean setAsDefault);

    void deleteCv(UUID userUuid, Long cvId);
    void deleteCv(Long userId, Long cvId);

    CandidateCvResponse setDefaultCv(UUID userUuid, Long cvId);
    CandidateCvResponse setDefaultCv(Long userId, Long cvId);

    InternalCvResponse getCvForInternalVerification(Long cvId);

    com.recruitment.candidate.dto.response.CandidateExportResponse exportData(UUID userUuid);

    void deleteAccount(UUID userUuid);

    void anonymizeCandidate(UUID userUuid);

    /** Invoked by scheduler: sends warning notifications to candidates expiring in 3 days. */
    int notifyExpiringOpenToWorkStatuses();

    /** Invoked by scheduler: reverts openToWork=false and sends notifications for candidates whose window has elapsed. */
    int expireOpenToWorkStatuses();
}
