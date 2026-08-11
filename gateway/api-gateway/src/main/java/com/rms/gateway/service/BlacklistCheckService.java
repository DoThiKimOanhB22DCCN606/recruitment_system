package com.rms.gateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class BlacklistCheckService {

    private static final Logger log = LoggerFactory.getLogger(BlacklistCheckService.class);

    private final ReactiveStringRedisTemplate redisTemplate;

    public BlacklistCheckService(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Mono<Boolean> isBlacklisted(UUID userId, UUID tenantId) {
        String userKey = "jwt_blacklist:user:" + userId;
        String tenantKey = "jwt_blacklist:tenant:" + tenantId;

        return redisTemplate.hasKey(userKey)
                .flatMap(isUserBlacklisted -> {
                    if (Boolean.TRUE.equals(isUserBlacklisted)) return Mono.just(true);
                    if (tenantId == null) return Mono.just(false);
                    return redisTemplate.hasKey(tenantKey);
                })
                .onErrorResume(e -> {
                    log.warn("Blacklist check Redis failure, failing open for user {}: {}", userId, e.getMessage());
                    return Mono.just(false);
                });
    }
}
