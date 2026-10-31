package com.rms.iam.service;

import com.rms.iam.entity.CompanyApprovalEmail;
import com.rms.iam.repository.CompanyApprovalEmailRepository;
import com.rms.iam.scheduler.CompanyApprovalEmailScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CompanyApprovalEmailTest {
    @Test
    void approvedApplicantReceivesLoginInstructions() {
        JavaMailSender sender = mock(JavaMailSender.class);
        RegistrationOtpEmailService service = new RegistrationOtpEmailService(
                sender, "verified@example.com", "http://localhost:5173");

        service.sendCompanyApproval("hr@example.com", "Example Co");

        org.mockito.ArgumentCaptor<SimpleMailMessage> captor =
                org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        assertArrayEquals(new String[]{"hr@example.com"}, captor.getValue().getTo());
        assertEquals("verified@example.com", captor.getValue().getFrom());
        assertTrue(captor.getValue().getText().contains("Example Co"));
        assertTrue(captor.getValue().getText().contains("http://localhost:5173/login"));
    }

    @Test
    void failedDeliveryStaysPendingForRetry() {
        CompanyApprovalEmailRepository repository = mock(CompanyApprovalEmailRepository.class);
        RegistrationOtpEmailService emailService = mock(RegistrationOtpEmailService.class);
        CompanyApprovalEmail email = new CompanyApprovalEmail();
        email.setUserId(UUID.randomUUID());
        email.setRecipientEmail("hr@example.com");
        email.setCompanyName("Example Co");
        when(repository.findTop20BySentAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(email));
        doThrow(new IllegalStateException("SMTP unavailable"))
                .when(emailService).sendCompanyApproval(any(), any());

        new CompanyApprovalEmailScheduler(repository, emailService).sendPendingEmails();

        assertNull(email.getSentAt());
        assertEquals(1, email.getAttemptCount());
        verify(repository).save(email);
    }
}
