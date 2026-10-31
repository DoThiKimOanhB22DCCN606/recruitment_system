package com.rms.iam.service;

import com.rms.iam.entity.AuditLog;
import com.rms.iam.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock private AuditLogRepository auditLogRepository;

    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        auditLogService = new AuditLogService(auditLogRepository);
    }

    @Test
    void searchLogsReturnsPagedResults() {
        UUID tenantId = UUID.randomUUID();
        AuditLog log = new AuditLog(UUID.randomUUID(), tenantId, UUID.randomUUID(),
                "JOB_SIGNIFICANT_FIELD_EDITED", "JOB", "101", null, null, "127.0.0.1", "Chrome", Instant.now());

        when(auditLogRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(log)));

        Page<AuditLog> page = auditLogService.searchLogs(tenantId, null, null, null, null, null, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getActionType()).isEqualTo("JOB_SIGNIFICANT_FIELD_EDITED");
    }

    @Test
    void exportLogsGeneratesValidCsv() {
        UUID logId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        AuditLog log = new AuditLog(logId, tenantId, actorId,
                "CROSS_TENANT_ATTEMPT", "APPLICATION", "202", null, null, "192.168.1.1", "Firefox", Instant.parse("2026-08-18T10:00:00Z"));

        when(auditLogRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(log));

        List<AuditLog> list = auditLogService.exportLogs(tenantId, null, null, null, null, null);
        String csv = auditLogService.toCsv(list);

        assertThat(csv).contains("id,tenant_id,actor_id,action_type,entity_type,entity_id,ip_address,created_at");
        assertThat(csv).contains(logId.toString());
        assertThat(csv).contains("CROSS_TENANT_ATTEMPT");
    }
}
