package com.rms.iam.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;

    public TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void blacklistUser(UUID userId, long expirationSeconds) {
        redisTemplate.opsForValue().set(
                "jwt_blacklist:user:" + userId,
                "blacklisted",
                Duration.ofSeconds(expirationSeconds)
        );
    }

    public void blacklistTenant(UUID tenantId, long expirationSeconds) {
        redisTemplate.opsForValue().set(
                "jwt_blacklist:tenant:" + tenantId,
                "blacklisted",
                Duration.ofSeconds(expirationSeconds)
        );
    }
}
