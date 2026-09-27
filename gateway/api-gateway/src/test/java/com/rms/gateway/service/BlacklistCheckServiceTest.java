package com.rms.gateway.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlacklistCheckServiceTest {

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    private BlacklistCheckService service;

    @BeforeEach
    void setUp() {
        service = new BlacklistCheckService(redisTemplate);
    }

    @Test
    void userBlacklistedReturnsTrue() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        when(redisTemplate.hasKey("jwt_blacklist:user:" + userId)).thenReturn(Mono.just(true));

        StepVerifier.create(service.isBlacklisted(userId, tenantId))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void notBlacklistedReturnsFalse() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        when(redisTemplate.hasKey("jwt_blacklist:user:" + userId)).thenReturn(Mono.just(false));
        when(redisTemplate.hasKey("jwt_blacklist:tenant:" + tenantId)).thenReturn(Mono.just(false));

        StepVerifier.create(service.isBlacklisted(userId, tenantId))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void redisErrorFailsClosedReturnsTrue() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.error(new RuntimeException("Redis connection refused")));

        StepVerifier.create(service.isBlacklisted(userId, tenantId))
                .expectNext(true)
                .verifyComplete();
    }
}