package com.rms.iam.scheduler;

import com.rms.iam.entity.User;
import com.rms.iam.entity.UserStatus;
import com.rms.iam.messaging.IamEventPublisher;
import com.rms.iam.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class UserAnonymizationScheduler {

    private static final Logger log = LoggerFactory.getLogger(UserAnonymizationScheduler.class);

    private final UserRepository userRepository;
    private final IamEventPublisher iamEventPublisher;

    public UserAnonymizationScheduler(UserRepository userRepository, IamEventPublisher iamEventPublisher) {
        this.userRepository = userRepository;
        this.iamEventPublisher = iamEventPublisher;
    }

    @Scheduled(cron = "${app.scheduler.anonymization-cron:0 0 3 * * *}") // Runs daily at 3 AM UTC
    @Transactional
    public int anonymizeDeactivatedUsers() {
        log.info("Running scheduled task: anonymizeDeactivatedUsers");
        Instant threshold = Instant.now().minus(30, ChronoUnit.DAYS);
        List<User> usersToAnonymize = userRepository.findDeactivatedUsersOlderThan(UserStatus.DEACTIVATED, threshold);

        if (usersToAnonymize.isEmpty()) {
            log.info("No deactivated users older than 30 days found for anonymization.");
            return 0;
        }

        for (User user : usersToAnonymize) {
            String originalEmail = user.getEmail();
            String anonymizedEmail = "anonymized_" + user.getId() + "@deleted.local";
            user.setEmail(anonymizedEmail);
            user.setPasswordHash("$2a$12$DUMMY_ANONYMIZED_HASH_DO_NOT_USE");
            user.setPendingCompanyName(null);
            userRepository.save(user);

            iamEventPublisher.publishUserAnonymizedEvent(user.getId(), anonymizedEmail);
            log.info("Anonymized user ID: {}, previous email: {}", user.getId(), originalEmail);
        }

        log.info("Completed anonymization for {} users.", usersToAnonymize.size());
        return usersToAnonymize.size();
    }
}
