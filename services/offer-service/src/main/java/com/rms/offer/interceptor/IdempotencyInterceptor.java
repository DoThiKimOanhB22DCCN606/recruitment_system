package com.rms.offer.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

@Component
public class IdempotencyInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;

    public IdempotencyInterceptor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String idempotencyKey = request.getHeader("Idempotency-Key");
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing Idempotency-Key header");
            return false;
        }

        String tenantId = request.getHeader("X-Tenant-Id");
        String userId = request.getHeader("X-User-Id");
        String redisKey = "idempotency:" + (tenantId != null ? tenantId : "global") + ":" + (userId != null ? userId : "anon") + ":" + idempotencyKey;

        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(redisKey, "processed", Duration.ofHours(24));
        if (Boolean.FALSE.equals(isNew)) {
            response.sendError(HttpServletResponse.SC_CONFLICT, "Duplicate request");
            return false;
        }

        return true;
    }
}
