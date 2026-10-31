package com.rms.job.scheduler;

import com.rms.job.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs on the cron defined by app.job.scheduler.cron.
 * Automatically closes PUBLISHED jobs once their expiresAt timestamp has passed.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JobExpiryScheduler {

    private final JobService jobService;

    @Scheduled(cron = "${app.job.scheduler.cron:0 * * * * *}", zone = "Asia/Ho_Chi_Minh")
    public void closeExpiredJobs() {
        int closed = jobService.closeExpiredJobs();
        if (closed > 0) {
            log.info("Job scheduler closed {} expired job posting(s)", closed);
        }
    }
}
