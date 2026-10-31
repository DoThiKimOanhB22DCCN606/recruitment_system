package com.rms.job;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableJpaRepositories(basePackages = "com.rms.job.repository")
@EnableElasticsearchRepositories(basePackages = "com.rms.job.repository.search")
public class JobServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(JobServiceApplication.class, args);
    }
}
