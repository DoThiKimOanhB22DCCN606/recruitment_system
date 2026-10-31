package com.rms.offer.config;

import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.core.LockProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class ShedLockConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration.builder()
            .withJdbcTemplate(new org.springframework.jdbc.core.JdbcTemplate(dataSource))
            .withTableName("offer_db.shedlock")
            .usingDbTime()
            .build()
        );
    }
}
