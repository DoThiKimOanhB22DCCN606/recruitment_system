package com.rms.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.util.Map;

@Configuration
public class RateLimiterConfig {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterConfig.class);

    @Bean
    @Primary
    public RateLimiter<RedisRateLimiter.Config> failOpenRateLimiter(RedisRateLimiter delegate) {
        return new RateLimiter<RedisRateLimiter.Config>() {
            @Override
            public Mono<Response> isAllowed(String routeId, String id) {
                return delegate.isAllowed(routeId, id)
                        .onErrorResume(Exception.class, e -> {
                            log.warn("Rate limiter Redis failure, failing open: {}", e.getMessage());
                            // FINDING-05: Fail open on Redis connection failure
                            return Mono.just(new Response(true, Map.of()));
                        });
            }

            @Override
            public Map<String, RedisRateLimiter.Config> getConfig() {
                return delegate.getConfig();
            }

            @Override
            public Class<RedisRateLimiter.Config> getConfigClass() {
                return delegate.getConfigClass();
            }

            @Override
            public RedisRateLimiter.Config newConfig() {
                return delegate.newConfig();
            }
        };
    }
}
