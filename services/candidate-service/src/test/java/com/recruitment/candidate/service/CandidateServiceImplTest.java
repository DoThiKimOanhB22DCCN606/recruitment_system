package com.recruitment.candidate.service;

import com.recruitment.candidate.client.AtsServiceClient;
import com.recruitment.candidate.client.IamServiceClient;
import com.recruitment.candidate.dto.request.CandidateProfileUpdateRequest;
import com.recruitment.candidate.dto.response.CandidateCvResponse;
import com.recruitment.candidate.dto.response.CandidateExportResponse;
import com.recruitment.candidate.dto.response.CandidateProfileResponse;
import com.recruitment.candidate.entity.Candidate;
import com.recruitment.candidate.entity.CandidateCv;
import com.recruitment.candidate.minio.MinioStorageService;
import com.recruitment.candidate.repository.CandidateCvRepository;
import com.recruitment.candidate.repository.CandidateRepository;
import com.recruitment.candidate.service.impl.CandidateServiceImpl;
import com.rms.common.exception.ErrorCode;
import com.rms.common.exception.RmsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidateServiceImplTest {

    @Mock private CandidateRepository candidateRepository;
    @Mock private CandidateCvRepository cvRepository;
    @Mock private com.recruitment.candidate.repository.SkillRepository skillRepository;
    @Mock private MinioStorageService minioStorageService;
    @Mock private AtsServiceClient atsServiceClient;
    @Mock private IamServiceClient iamServiceClient;
    @Mock private com.recruitment.candidate.messaging.NotificationPublisher notificationPublisher;

    private CandidateService candidateService;
    private Candidate candidate;

    @BeforeEach
    void setUp() {
        candidateService = new CandidateServiceImpl(
                candidateRepository,
                cvRepository,
                skillRepository,
                minioStorageService,
                new CandidateMapper(),
                atsServiceClient,
                iamServiceClient,
                notificationPublisher
        );

        candidate = Candidate.builder()
                .id(1L)
                .userId(100L)
                .userUuid(UUID.fromString("00000000-0000-0000-0000-000000000100"))
                .fullName("John Doe")
                .email("john@example.com")
                .phone("+84901234567")
                .build();
    }

    @Test
    void uploadValidPdfCvSuccess() {
        when(candidateRepository.findByUserId(100L)).thenReturn(Optional.of(candidate));
        when(cvRepository.countByCandidateId(1L)).thenReturn(1L);
        when(minioStorageService.uploadCv(eq(1L), any())).thenReturn("cvs/1/test.pdf");
        when(minioStorageService.generatePresignedDownloadUrl(any())).thenReturn("http://download-url");

        byte[] validPdfBytes = new byte[] {0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34}; // "%PDF-1.4"
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", validPdfBytes);

        when(cvRepository.save(any(CandidateCv.class))).thenAnswer(inv -> {
            CandidateCv cv = inv.getArgument(0);
            cv.setId(50L);
            return cv;
        });

        CandidateCvResponse response = candidateService.uploadCv(100L, file, false);

        assertThat(response.getId()).isEqualTo(50L);
        assertThat(response.getFileName()).isEqualTo("resume.pdf");
        verify(minioStorageService).uploadCv(eq(1L), any());
    }

    @Test
    void uploadCvFailsWhenFileSizeExceeds5Mb() {
        when(candidateRepository.findByUserId(100L)).thenReturn(Optional.of(candidate));

        byte[] largeBytes = new byte[5_242_881]; // 5MB + 1 byte
        largeBytes[0] = 0x25; largeBytes[1] = 0x50; largeBytes[2] = 0x44; largeBytes[3] = 0x46;
        MockMultipartFile file = new MockMultipartFile("file", "large.pdf", "application/pdf", largeBytes);

        assertThatThrownBy(() -> candidateService.uploadCv(100L, file, false))
                .isInstanceOf(RmsException.class)
                .satisfies(e -> assertThat(((RmsException) e).getErrorCode()).isEqualTo(ErrorCode.ERR_FILE_TOO_LARGE));
    }

    @Test
    void uploadCvFailsWhenMagicBytesAreInvalid() {
        when(candidateRepository.findByUserId(100L)).thenReturn(Optional.of(candidate));

        byte[] fakePdfBytes = "NOT A PDF FILE".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "fake.pdf", "application/pdf", fakePdfBytes);

        assertThatThrownBy(() -> candidateService.uploadCv(100L, file, false))
                .isInstanceOf(RmsException.class)
                .satisfies(e -> assertThat(((RmsException) e).getErrorCode()).isEqualTo(ErrorCode.ERR_INVALID_FILE_TYPE));
    }

    @Test
    void uploadCvFailsWhenCvCapOf3IsReached() {
        when(candidateRepository.findByUserId(100L)).thenReturn(Optional.of(candidate));
        when(cvRepository.countByCandidateId(1L)).thenReturn(3L);

        byte[] validPdfBytes = new byte[] {0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34};
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", validPdfBytes);

        assertThatThrownBy(() -> candidateService.uploadCv(100L, file, false))
                .isInstanceOf(RmsException.class)
                .satisfies(e -> assertThat(((RmsException) e).getErrorCode()).isEqualTo(ErrorCode.ERR_CV_LIMIT_EXCEEDED));
    }

    @Test
    void deleteCvFailsWhenCvIsInUseInActiveApplication() {
        CandidateCv cv = CandidateCv.builder()
                .id(10L)
                .candidate(candidate)
                .objectKey("cvs/1/cv1.pdf")
                .build();

        when(candidateRepository.findByUserId(100L)).thenReturn(Optional.of(candidate));
        when(cvRepository.findById(10L)).thenReturn(Optional.of(cv));
        when(atsServiceClient.isCvInUse(10L)).thenReturn(true);

        assertThatThrownBy(() -> candidateService.deleteCv(100L, 10L))
                .isInstanceOf(RmsException.class)
                .satisfies(e -> assertThat(((RmsException) e).getErrorCode()).isEqualTo(ErrorCode.ERR_CV_IN_USE));
    }

    @Test
    void deleteDefaultCvReassignsDefaultToRemainingCv() {
        CandidateCv cv1 = CandidateCv.builder()
                .id(10L)
                .candidate(candidate)
                .objectKey("cvs/1/cv1.pdf")
                .isDefault(true)
                .build();

        CandidateCv cv2 = CandidateCv.builder()
                .id(20L)
                .candidate(candidate)
                .objectKey("cvs/1/cv2.pdf")
                .isDefault(false)
                .build();

        when(candidateRepository.findByUserId(100L)).thenReturn(Optional.of(candidate));
        when(cvRepository.findById(10L)).thenReturn(Optional.of(cv1));
        when(atsServiceClient.isCvInUse(10L)).thenReturn(false);
        when(cvRepository.findByCandidateId(1L)).thenReturn(List.of(cv2));

        candidateService.deleteCv(100L, 10L);

        verify(minioStorageService).deleteObject("cvs/1/cv1.pdf");
        verify(cvRepository).delete(cv1);
        assertThat(cv2.isDefault()).isTrue();
        verify(cvRepository).save(cv2);
    }

    @Test
    void exportDataReturnsStructuredPersonalData() {
        UUID userUuid = UUID.fromString("00000000-0000-0000-0000-000000000100");
        when(candidateRepository.findByUserUuid(userUuid)).thenReturn(Optional.of(candidate));
        when(cvRepository.findByCandidateId(1L)).thenReturn(List.of());

        CandidateExportResponse exportResponse = candidateService.exportData(userUuid);

        assertThat(exportResponse).isNotNull();
        assertThat(exportResponse.getProfile().getFullName()).isEqualTo("John Doe");
        assertThat(exportResponse.getProfile().getEmail()).isEqualTo("john@example.com");
        assertThat(exportResponse.getExportedAt()).isNotNull();
    }

    @Test
    void deleteAccountDeactivatesUserWithoutImmediateAnonymization() {
        UUID userUuid = UUID.fromString("00000000-0000-0000-0000-000000000100");
        candidate.setOpenToWork(true);
        when(candidateRepository.findByUserUuid(userUuid)).thenReturn(Optional.of(candidate));

        candidateService.deleteAccount(userUuid);

        verify(iamServiceClient).deactivateUser(userUuid);
        verify(minioStorageService, never()).deleteObject(any());
        verify(cvRepository, never()).deleteAll(anyList());
        assertThat(candidate.getFullName()).isEqualTo("John Doe");
        assertThat(candidate.getEmail()).isEqualTo("john@example.com");
        assertThat(candidate.isOpenToWork()).isFalse();
        verify(candidateRepository).save(candidate);
    }

    @Test
    void anonymizeCandidateErasesCvsAndAnonymizesPii() {
        UUID userUuid = UUID.fromString("00000000-0000-0000-0000-000000000100");
        CandidateCv cv = CandidateCv.builder()
                .id(11L)
                .candidate(candidate)
                .objectKey("cvs/1/cv.pdf")
                .build();

        when(candidateRepository.findByUserUuid(userUuid)).thenReturn(Optional.of(candidate));
        when(cvRepository.findByCandidateId(1L)).thenReturn(List.of(cv));

        candidateService.anonymizeCandidate(userUuid);

        verify(minioStorageService).deleteObject("cvs/1/cv.pdf");
        verify(cvRepository).deleteAll(anyList());
        assertThat(candidate.getFullName()).isEqualTo("Anonymized Candidate");
        assertThat(candidate.getEmail()).contains("anonymized_1@deleted.local");
        assertThat(candidate.getPhone()).isNull();
        verify(candidateRepository).save(candidate);
    }

    @Test
    void openToWorkWarningAndExpiryPublishesNotifications() {
        candidate.setOpenToWork(true);
        candidate.setOpenToWorkUntil(java.time.LocalDateTime.now().minusHours(1));

        when(candidateRepository.findByOpenToWorkTrueAndOpenToWorkUntilBetweenAndWarningSentAtIsNull(any(), any()))
                .thenReturn(List.of(candidate));
        when(candidateRepository.findByOpenToWorkTrueAndOpenToWorkUntilBefore(any()))
                .thenReturn(List.of(candidate));

        int warningCount = candidateService.notifyExpiringOpenToWorkStatuses();
        assertThat(warningCount).isEqualTo(1);

        int expireCount = candidateService.expireOpenToWorkStatuses();
        assertThat(expireCount).isEqualTo(1);

        verify(notificationPublisher, times(2)).publish(any(com.recruitment.candidate.messaging.NotificationEvent.class));
        assertThat(candidate.isOpenToWork()).isFalse();
    }

    @Test
    void submittingKnownSkillSucceeds() {
        when(candidateRepository.findByUserId(100L)).thenReturn(Optional.of(candidate));
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(inv -> inv.getArgument(0));
        when(skillRepository.findByNameIgnoreCase("Java"))
                .thenReturn(Optional.of(new com.recruitment.candidate.entity.Skill(1L, "Java")));

        CandidateProfileUpdateRequest request = new CandidateProfileUpdateRequest();
        CandidateProfileUpdateRequest.SkillRequest skillReq = new CandidateProfileUpdateRequest.SkillRequest();
        skillReq.setSkillName("Java");
        skillReq.setProficiency("EXPERT");
        request.setSkills(List.of(skillReq));

        CandidateProfileResponse response = candidateService.updateProfile(100L, request);
        assertThat(response.getSkills()).hasSize(1);
        assertThat(response.getSkills().get(0).getSkillName()).isEqualTo("Java");
    }

    @Test
    void submittingUnknownSkillThrows400() {
        when(candidateRepository.findByUserId(100L)).thenReturn(Optional.of(candidate));
        when(skillRepository.findByNameIgnoreCase("UnknownSkill123")).thenReturn(Optional.empty());

        CandidateProfileUpdateRequest request = new CandidateProfileUpdateRequest();
        CandidateProfileUpdateRequest.SkillRequest skillReq = new CandidateProfileUpdateRequest.SkillRequest();
        skillReq.setSkillName("UnknownSkill123");
        skillReq.setProficiency("BEGINNER");
        request.setSkills(List.of(skillReq));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> candidateService.updateProfile(100L, request))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .satisfies(e -> {
                    org.springframework.web.server.ResponseStatusException rse = (org.springframework.web.server.ResponseStatusException) e;
                    assertThat(rse.getStatusCode().value()).isEqualTo(400);
                });
    }
}
