package com.recruitment.candidate.scheduler;

import com.recruitment.candidate.service.CandidateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Automatically turns off "Open To Work" once its expiry window passes. */
@Component
@RequiredArgsConstructor
@Slf4j
public class OpenToWorkExpiryScheduler {

    private final CandidateService candidateService;

    @Scheduled(cron = "${app.open-to-work.scheduler.cron:0 0 * * * *}")
    public void expireOpenToWork() {
        int count = candidateService.expireOpenToWorkStatuses();
        if (count > 0) {
            log.info("Expired 'Open To Work' status for {} candidate(s)", count);
        }
    }
}
