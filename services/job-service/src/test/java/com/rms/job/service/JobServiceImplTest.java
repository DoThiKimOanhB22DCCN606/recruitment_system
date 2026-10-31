package com.rms.job.service;

import com.rms.job.dto.request.JobCreateRequest;
import com.rms.job.dto.request.JobStatusChangeRequest;
import com.rms.job.dto.response.JobResponse;
import com.rms.job.entity.Job;
import com.rms.job.entity.JobStatus;
import com.rms.job.messaging.JobEventPublisher;
import com.rms.job.repository.JobCategoryRepository;
import com.rms.job.repository.JobRepository;
import com.rms.job.repository.JobStatusHistoryRepository;
import com.rms.job.service.impl.JobServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceImplTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID RECRUITER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID HR_ADMIN_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock private JobRepository jobRepository;
    @Mock private JobCategoryRepository jobCategoryRepository;
    @Mock private JobStatusHistoryRepository historyRepository;
    @Mock private com.rms.job.repository.AuditLogRepository auditLogRepository;
    @Mock private com.rms.job.repository.search.JobSearchRepository jobSearchRepository;
    @Mock private JobEventPublisher jobEventPublisher;
    @Mock private com.rms.job.client.AtsClient atsClient;
    @Mock private com.rms.job.client.CompanyClient companyClient;

    private JobService jobService;

    @BeforeEach
    void setUp() {
        jobService = new JobServiceImpl(
                jobRepository,
                jobCategoryRepository,
                historyRepository,
                auditLogRepository,
                jobSearchRepository,
                new JobMapper(),
                jobEventPublisher,
                atsClient,
                companyClient,
                new com.fasterxml.jackson.databind.ObjectMapper()
        );
        lenient().when(companyClient.getCompanyByTenant(eq(TENANT_ID), any(UUID.class)))
                .thenReturn(new com.rms.job.client.CompanyClient.CompanyInfo(42L, "Shared Company", null, true));
    }

    @Test
    void hrAdminCreatesJobAsDraft() {
        JobCreateRequest request = new JobCreateRequest();
        request.setTitle("Senior Java Engineer");
        request.setDescription("Design and build scalable systems");
        request.setLocation("Remote");
        request.setEmploymentType(Job.EmploymentType.FULL_TIME);
        request.setSalaryMin(BigDecimal.valueOf(2000));
        request.setSalaryMax(BigDecimal.valueOf(4000));

        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> {
            Job job = inv.getArgument(0);
            job.setId(101L);
            return job;
        });

        JobResponse response = jobService.createJob(request, HR_ADMIN_ID, TENANT_ID, "HR_ADMIN");

        assertThat(response.getStatus()).isEqualTo(JobStatus.DRAFT);
        assertThat(response.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(response.getCompanyId()).isEqualTo(42L);
        assertThat(response.getCompanyName()).isEqualTo("Shared Company");
        verify(jobEventPublisher).publishJobCreated(eq(101L), eq(TENANT_ID), eq("Senior Java Engineer"), eq(HR_ADMIN_ID));
    }

    @Test
    void recruiterCreatesJobAsDraft() {
        JobCreateRequest request = new JobCreateRequest();
        request.setTitle("Junior Frontend Engineer");
        request.setDescription("React / TypeScript");
        request.setLocation("Hanoi");
        request.setEmploymentType(Job.EmploymentType.FULL_TIME);

        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> {
            Job job = inv.getArgument(0);
            job.setId(102L);
            return job;
        });

        JobResponse response = jobService.createJob(request, RECRUITER_ID, TENANT_ID, "RECRUITER");

        assertThat(response.getStatus()).isEqualTo(JobStatus.DRAFT);
        verify(jobEventPublisher).publishJobCreated(eq(102L), eq(TENANT_ID), eq("Junior Frontend Engineer"), eq(RECRUITER_ID));
    }

    @Test
    void createJobFailsWhenSalaryMinExceedsSalaryMax() {
        JobCreateRequest request = new JobCreateRequest();
        request.setTitle("QA Engineer");
        request.setDescription("Automated testing");
        request.setLocation("Hanoi");
        request.setEmploymentType(Job.EmploymentType.FULL_TIME);
        request.setSalaryMin(BigDecimal.valueOf(5000));
        request.setSalaryMax(BigDecimal.valueOf(3000));

        assertThatThrownBy(() -> jobService.createJob(request, RECRUITER_ID, TENANT_ID, "RECRUITER"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Minimum salary cannot exceed maximum salary");
    }

    @Test
    void hrAdminApprovesPendingJob() {
        Job job = Job.builder()
                .id(103L)
                .tenantId(TENANT_ID)
                .recruiterId(RECRUITER_ID)
                .title("DevOps Engineer")
                .status(JobStatus.PENDING_APPROVAL)
                .build();

        when(jobRepository.findByIdAndTenantId(103L, TENANT_ID)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        JobStatusChangeRequest request = new JobStatusChangeRequest();
        request.setTargetStatus(JobStatus.PUBLISHED);

        JobResponse response = jobService.changeStatus(103L, request, HR_ADMIN_ID, TENANT_ID, "HR_ADMIN");

        assertThat(response.getStatus()).isEqualTo(JobStatus.PUBLISHED);
        verify(jobEventPublisher).publishApprovalOutcome(eq(103L), eq(TENANT_ID), eq(RECRUITER_ID), eq("APPROVED"), isNull());
    }

    @Test
    void publishingPreservesSelectedDeadlineAndClosingKeepsJobVisible() {
        LocalDateTime deadline = LocalDateTime.now().plusDays(12);
        Job job = Job.builder().id(109L).tenantId(TENANT_ID).title("Designer")
                .status(JobStatus.PENDING_APPROVAL).expiresAt(deadline).build();
        when(jobRepository.findByIdAndTenantId(109L, TENANT_ID)).thenReturn(Optional.of(job));
        when(jobRepository.findById(109L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        JobStatusChangeRequest publish = new JobStatusChangeRequest();
        publish.setTargetStatus(JobStatus.PUBLISHED);
        assertThat(jobService.changeStatus(109L, publish, HR_ADMIN_ID, TENANT_ID, "HR_ADMIN").getExpiresAt())
                .isEqualTo(deadline);

        JobStatusChangeRequest close = new JobStatusChangeRequest();
        close.setTargetStatus(JobStatus.CLOSED);
        assertThat(jobService.changeStatus(109L, close, HR_ADMIN_ID, TENANT_ID, "HR_ADMIN").getStatus())
                .isEqualTo(JobStatus.CLOSED);
        assertThat(jobService.getJobById(109L, null, null).getStatus()).isEqualTo(JobStatus.CLOSED);
        verify(jobSearchRepository, times(2)).save(any());
        verifyNoInteractions(atsClient);
    }

    @Test
    void recruiterCannotStopPublishedJob() {
        Job job = Job.builder().id(110L).tenantId(TENANT_ID).status(JobStatus.PUBLISHED).build();
        when(jobRepository.findByIdAndTenantId(110L, TENANT_ID)).thenReturn(Optional.of(job));
        JobStatusChangeRequest close = new JobStatusChangeRequest();
        close.setTargetStatus(JobStatus.CLOSED);

        assertThatThrownBy(() -> jobService.changeStatus(110L, close, RECRUITER_ID, TENANT_ID, "RECRUITER"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only HR_ADMIN");
        verify(jobRepository, never()).save(any());
    }

    @Test
    void hrAdminRejectsPendingJobWithMandatoryNote() {
        Job job = Job.builder()
                .id(104L)
                .tenantId(TENANT_ID)
                .recruiterId(RECRUITER_ID)
                .title("Marketing Lead")
                .status(JobStatus.PENDING_APPROVAL)
                .build();

        when(jobRepository.findByIdAndTenantId(104L, TENANT_ID)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        JobStatusChangeRequest request = new JobStatusChangeRequest();
        request.setTargetStatus(JobStatus.DRAFT);
        request.setNote("Salary range does not meet departmental budget.");

        JobResponse response = jobService.changeStatus(104L, request, HR_ADMIN_ID, TENANT_ID, "HR_ADMIN");

        assertThat(response.getStatus()).isEqualTo(JobStatus.DRAFT);
        verify(jobEventPublisher).publishApprovalOutcome(eq(104L), eq(TENANT_ID), eq(RECRUITER_ID), eq("REJECTED"), eq("Salary range does not meet departmental budget."));
    }

    @Test
    void createAndFetchJobWithWorkTypeEnum() {
        JobCreateRequest request = new JobCreateRequest();
        request.setTitle("Backend Engineer");
        request.setDescription("Spring Boot & Postgres");
        request.setLocation("Danang");
        request.setEmploymentType(Job.EmploymentType.FULL_TIME);
        request.setWorkType(Job.WorkType.HYBRID);

        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> {
            Job job = inv.getArgument(0);
            job.setId(105L);
            return job;
        });

        JobResponse response = jobService.createJob(request, HR_ADMIN_ID, TENANT_ID, "HR_ADMIN");

        assertThat(response.getWorkType()).isEqualTo(Job.WorkType.HYBRID);
    }

    @Test
    void significantFieldChangeOnPublishedJobWritesAuditLog() {
        Job job = Job.builder()
                .id(106L)
                .tenantId(TENANT_ID)
                .title("Original Title")
                .location("Hanoi")
                .status(JobStatus.PUBLISHED)
                .build();

        when(jobRepository.findByIdAndTenantId(106L, TENANT_ID)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        com.rms.job.dto.request.JobUpdateRequest updateRequest = new com.rms.job.dto.request.JobUpdateRequest();
        updateRequest.setTitle("New Title");

        jobService.updateJob(106L, updateRequest, HR_ADMIN_ID, TENANT_ID, "HR_ADMIN");

        org.mockito.ArgumentCaptor<com.rms.job.entity.AuditLog> captor =
                org.mockito.ArgumentCaptor.forClass(com.rms.job.entity.AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getActionType()).isEqualTo("JOB_SIGNIFICANT_FIELD_EDITED");
        assertThat(captor.getValue().getEntityId()).isEqualTo("106");
        assertThat(captor.getValue().getTenantId()).isEqualTo(TENANT_ID);
        assertThat(captor.getValue().getBeforeState()).contains("Original Title");
        assertThat(captor.getValue().getAfterState()).contains("New Title");
    }

    @Test
    void partialSalaryUpdateCrossValidationThrowsWhenMinExceedsStoredMax() {
        Job job = Job.builder()
                .id(108L)
                .tenantId(TENANT_ID)
                .salaryMin(java.math.BigDecimal.valueOf(1000))
                .salaryMax(java.math.BigDecimal.valueOf(2000))
                .status(JobStatus.PUBLISHED)
                .build();

        when(jobRepository.findByIdAndTenantId(108L, TENANT_ID)).thenReturn(Optional.of(job));

        com.rms.job.dto.request.JobUpdateRequest updateRequest = new com.rms.job.dto.request.JobUpdateRequest();
        updateRequest.setSalaryMin(java.math.BigDecimal.valueOf(3000)); // Exceeds stored max 2000

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                jobService.updateJob(108L, updateRequest, HR_ADMIN_ID, TENANT_ID, "HR_ADMIN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Minimum salary cannot exceed maximum salary");
    }

    @Test
    void editOnDraftJobDoesNotWriteAuditLog() {
        Job job = Job.builder()
                .id(107L)
                .tenantId(TENANT_ID)
                .title("Draft Title")
                .location("Hanoi")
                .status(JobStatus.DRAFT)
                .build();

        when(jobRepository.findByIdAndTenantId(107L, TENANT_ID)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        com.rms.job.dto.request.JobUpdateRequest updateRequest = new com.rms.job.dto.request.JobUpdateRequest();
        updateRequest.setTitle("Updated Draft Title");

        jobService.updateJob(107L, updateRequest, HR_ADMIN_ID, TENANT_ID, "HR_ADMIN");

        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void newDraftIsNotShownInPublicSearch() {
        JobCreateRequest request = new JobCreateRequest();
        request.setTitle("DevOps Lead");
        request.setDescription("Kubernetes & AWS");
        request.setLocation("Hanoi");
        request.setEmploymentType(Job.EmploymentType.FULL_TIME);

        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> {
            Job job = inv.getArgument(0);
            job.setId(108L);
            return job;
        });

        jobService.createJob(request, HR_ADMIN_ID, TENANT_ID, "HR_ADMIN");

        verify(jobSearchRepository, never()).save(any(com.rms.job.document.JobDocument.class));
    }
}
