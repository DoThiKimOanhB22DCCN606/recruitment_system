package com.rms.offer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.rms.offer", "com.rms.common"})
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "15m")
public class OfferServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OfferServiceApplication.class, args);
    }
}
