package com.rms.iam.scheduler;

import com.rms.iam.entity.CompanyApprovalEmail;
import com.rms.iam.repository.CompanyApprovalEmailRepository;
import com.rms.iam.service.RegistrationOtpEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class CompanyApprovalEmailScheduler {
    private static final Logger log = LoggerFactory.getLogger(CompanyApprovalEmailScheduler.class);
    private final CompanyApprovalEmailRepository repository;
    private final RegistrationOtpEmailService emailService;

    public CompanyApprovalEmailScheduler(CompanyApprovalEmailRepository repository,
                                         RegistrationOtpEmailService emailService) {
        this.repository = repository;
        this.emailService = emailService;
    }

    @Scheduled(initialDelay = 5000, fixedDelay = 30000)
    public void sendPendingEmails() {
        for (CompanyApprovalEmail email : repository.findTop20BySentAtIsNullOrderByCreatedAtAsc()) {
            try {
                emailService.sendCompanyApproval(email.getRecipientEmail(), email.getCompanyName());
                email.setSentAt(Instant.now());
                repository.save(email);
            } catch (Exception ex) {
                email.setAttemptCount(email.getAttemptCount() + 1);
                repository.save(email);
                log.warn("Could not send company approval email for userId={}, retry in 30 seconds: {}",
                        email.getUserId(), ex.getMessage());
            }
        }
    }
}
