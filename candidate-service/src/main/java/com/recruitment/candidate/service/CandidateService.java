package com.recruitment.candidate.service;

import com.recruitment.candidate.dto.request.CandidateProfileUpdateRequest;
import com.recruitment.candidate.dto.request.OpenToWorkRequest;
import com.recruitment.candidate.dto.response.CandidateCvResponse;
import com.recruitment.candidate.dto.response.CandidateProfileResponse;
import com.recruitment.candidate.dto.response.InternalCvResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CandidateService {

    CandidateProfileResponse getProfile(Long userId);

    CandidateProfileResponse updateProfile(Long userId, CandidateProfileUpdateRequest request);

    CandidateProfileResponse setOpenToWork(Long userId, OpenToWorkRequest request);

    List<CandidateCvResponse> listCvs(Long userId);

    CandidateCvResponse uploadCv(Long userId, MultipartFile file, boolean setAsDefault);

    void deleteCv(Long userId, Long cvId);

    CandidateCvResponse setDefaultCv(Long userId, Long cvId);

    InternalCvResponse getCvForInternalVerification(Long cvId);

    /** Invoked by scheduler: reverts openToWork=false for candidates whose window has elapsed. */
    int expireOpenToWorkStatuses();
}
