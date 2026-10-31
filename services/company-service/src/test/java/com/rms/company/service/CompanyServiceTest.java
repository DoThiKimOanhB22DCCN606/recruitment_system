package com.rms.company.service;

import com.rms.company.client.TenantClient;
import com.rms.company.dto.request.CompanyProfileUpdateRequest;
import com.rms.company.dto.response.CompanyProfileResponse;
import com.rms.company.entity.Company;
import com.rms.company.messaging.UserCreatedEventListener;
import com.rms.company.minio.MinioStorageService;
import com.rms.company.repository.CompanyRepository;
import com.rms.company.service.impl.CompanyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock private CompanyRepository companyRepository;
    @Mock private MinioStorageService minioStorageService;
    @Mock private TenantClient tenantClient;

    private CompanyService companyService;
    private UserCreatedEventListener eventListener;

    @BeforeEach
    void setUp() {
        companyService = new CompanyServiceImpl(
                companyRepository,
                minioStorageService,
                new CompanyMapper(minioStorageService),
                tenantClient
        );
        eventListener = new UserCreatedEventListener(companyRepository, tenantClient);
    }

    @Test
    void getProfileCreatesCompanyIfAbsent() {
        when(companyRepository.findByOwnerUserId(500L)).thenReturn(Optional.empty());
        when(companyRepository.save(any(Company.class))).thenAnswer(inv -> {
            Company c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });

        CompanyProfileResponse response = companyService.getProfile(500L);

        assertThat(response.getId()).isEqualTo(10L);
        verify(companyRepository).save(any(Company.class));
    }

    @Test
    void updateProfileUpdatesFields() {
        Company existing = Company.builder()
                .id(1L)
                .ownerUserId(500L)
                .name("Old Name")
                .build();

        when(companyRepository.findByOwnerUserId(500L)).thenReturn(Optional.of(existing));
        when(companyRepository.save(any(Company.class))).thenAnswer(inv -> inv.getArgument(0));

        CompanyProfileUpdateRequest request = new CompanyProfileUpdateRequest();
        request.setName("Tech Corp");
        request.setDescription("Innovative solutions");
        request.setIndustry("Technology");

        CompanyProfileResponse response = companyService.updateProfile(500L, request);

        assertThat(response.getName()).isEqualTo("Tech Corp");
        assertThat(response.getDescription()).isEqualTo("Innovative solutions");
        verify(companyRepository).save(existing);
    }

    @Test
    void existingTenantCompanyUsesApprovedTenantName() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Company existing = Company.builder().id(4L).tenantId(tenantId)
                .ownerUserUuid(userId).name("New Company").build();
        when(tenantClient.getName(tenantId)).thenReturn("RMS Demo Company");
        when(companyRepository.findByTenantId(tenantId)).thenReturn(Optional.of(existing));
        when(companyRepository.save(existing)).thenReturn(existing);

        CompanyProfileResponse response = companyService.getProfile(tenantId, userId);

        assertThat(response.getName()).isEqualTo("RMS Demo Company");
        verify(companyRepository).save(existing);
    }

    @Test
    void profileNameChangeUpdatesTenantName() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Company existing = Company.builder().id(4L).tenantId(tenantId)
                .ownerUserUuid(userId).name("Old Company").build();
        when(tenantClient.getName(tenantId)).thenReturn("Old Company");
        when(companyRepository.findByTenantId(tenantId)).thenReturn(Optional.of(existing));
        when(companyRepository.save(existing)).thenReturn(existing);
        CompanyProfileUpdateRequest request = new CompanyProfileUpdateRequest();
        request.setName("New Company Name");

        CompanyProfileResponse response = companyService.updateProfile(tenantId, userId, request);

        assertThat(response.getName()).isEqualTo("New Company Name");
        verify(tenantClient).updateName(tenantId, "New Company Name");
    }

    @Test
    void eventListenerAutoCreatesCompanyOnEmployerRegistered() {
        UUID userId = UUID.randomUUID();
        when(companyRepository.findByOwnerUserUuid(userId)).thenReturn(Optional.empty());
        UUID tenantId = UUID.randomUUID();
        when(tenantClient.getName(tenantId)).thenReturn("ACME Corp");

        UserCreatedEventListener.UserCreatedEvent event = new UserCreatedEventListener.UserCreatedEvent(
                "USER_REGISTERED",
                userId,
                "recruiter@acme.com",
                "HR_ADMIN",
                tenantId,
                "corr-1",
                Instant.now()
        );

        eventListener.onUserCreated(event);

        verify(companyRepository).save(argThat(c -> c.getName().equals("ACME Corp") && userId.equals(c.getOwnerUserUuid())));
    }

    @Test
    void getProfileWithUuidCreatesCompanyIfAbsent() {
        UUID userUuid = UUID.randomUUID();
        when(companyRepository.findByOwnerUserUuid(userUuid)).thenReturn(Optional.empty());
        when(companyRepository.save(any(Company.class))).thenAnswer(inv -> {
            Company c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });

        CompanyProfileResponse response = companyService.getProfile(userUuid);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getOwnerUserUuid()).isEqualTo(userUuid);
        verify(companyRepository).save(any(Company.class));
    }

    @Test
    void uploadValidPngLogoSuccess() {
        Company company = Company.builder().id(1L).ownerUserId(500L).build();
        when(companyRepository.findByOwnerUserId(500L)).thenReturn(Optional.of(company));
        when(companyRepository.save(any(Company.class))).thenAnswer(inv -> inv.getArgument(0));
        when(minioStorageService.uploadLogo(eq(1L), any())).thenReturn("logos/1/logo.png");

        byte[] pngBytes = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        org.springframework.mock.web.MockMultipartFile file =
                new org.springframework.mock.web.MockMultipartFile("file", "logo.png", "image/png", pngBytes);

        CompanyProfileResponse response = companyService.uploadLogo(500L, file);

        assertThat(response).isNotNull();
        verify(minioStorageService).uploadLogo(eq(1L), any());
    }

    @Test
    void getLogoReadsTheAuthenticatedTenantsStoredObject() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String objectKey = "companies/4/logo/current.png";
        byte[] png = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47};
        Company company = Company.builder().id(4L).tenantId(tenantId)
                .ownerUserUuid(userId).name("RMS Demo Company")
                .logoObjectKey(objectKey).build();
        when(tenantClient.getName(tenantId)).thenReturn("RMS Demo Company");
        when(companyRepository.findByTenantId(tenantId)).thenReturn(Optional.of(company));
        when(minioStorageService.downloadLogo(objectKey)).thenReturn(png);

        assertThat(companyService.getLogo(tenantId, userId)).isEqualTo(png);
        verify(minioStorageService).downloadLogo(objectKey);
    }

    @Test
    void publicBrandAndLogoComeFromTheSameTenantProfile() {
        UUID tenantId = UUID.randomUUID();
        String objectKey = "companies/4/logo/current.png";
        byte[] png = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47};
        Company company = Company.builder().id(4L).tenantId(tenantId)
                .name("RMS Demo Company").logoObjectKey(objectKey).build();
        when(companyRepository.findByTenantId(tenantId)).thenReturn(Optional.of(company));
        when(minioStorageService.downloadLogo(objectKey)).thenReturn(png);

        assertThat(companyService.getBrandByTenant(tenantId).name()).isEqualTo("RMS Demo Company");
        assertThat(companyService.getBrandByTenant(tenantId).hasLogo()).isTrue();
        assertThat(companyService.getPublicLogoByTenant(tenantId)).isEqualTo(png);
    }

    @Test
    void uploadLogoFailsWhenSizeExceeds2Mb() {
        Company company = Company.builder().id(1L).ownerUserId(500L).build();
        when(companyRepository.findByOwnerUserId(500L)).thenReturn(Optional.of(company));

        byte[] largeBytes = new byte[2_097_153]; // 2MB + 1 byte
        largeBytes[0] = (byte) 0x89; largeBytes[1] = 0x50; largeBytes[2] = 0x4E; largeBytes[3] = 0x47;
        org.springframework.mock.web.MockMultipartFile file =
                new org.springframework.mock.web.MockMultipartFile("file", "large.png", "image/png", largeBytes);

        assertThatThrownBy(() -> companyService.uploadLogo(500L, file))
                .isInstanceOf(com.rms.common.exception.RmsException.class)
                .satisfies(e -> assertThat(((com.rms.common.exception.RmsException) e).getErrorCode())
                        .isEqualTo(com.rms.common.exception.ErrorCode.ERR_FILE_TOO_LARGE));
    }

    @Test
    void uploadLogoFailsWhenMagicBytesAreInvalid() {
        Company company = Company.builder().id(1L).ownerUserId(500L).build();
        when(companyRepository.findByOwnerUserId(500L)).thenReturn(Optional.of(company));

        byte[] fakeBytes = "NOT AN IMAGE".getBytes();
        org.springframework.mock.web.MockMultipartFile file =
                new org.springframework.mock.web.MockMultipartFile("file", "fake.png", "image/png", fakeBytes);

        assertThatThrownBy(() -> companyService.uploadLogo(500L, file))
                .isInstanceOf(com.rms.common.exception.RmsException.class)
                .satisfies(e -> assertThat(((com.rms.common.exception.RmsException) e).getErrorCode())
                        .isEqualTo(com.rms.common.exception.ErrorCode.ERR_INVALID_FILE_TYPE));
    }
}
