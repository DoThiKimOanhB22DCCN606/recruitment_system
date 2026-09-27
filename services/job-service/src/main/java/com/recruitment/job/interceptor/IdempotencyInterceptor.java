package com.recruitment.job.interceptor;

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
            return true;
        }

        String tenantId = request.getHeader("X-Tenant-Id");
        String userId = request.getHeader("X-User-Id");
        String redisKey = "idempotency:jobs:" + (tenantId != null ? tenantId : "global") + ":" + (userId != null ? userId : "anon") + ":" + idempotencyKey;

        try {
            Boolean isNew = redisTemplate.opsForValue().setIfAbsent(redisKey, "processed", Duration.ofHours(24));
            if (Boolean.FALSE.equals(isNew)) {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                response.setContentType("application/json");
                response.getWriter().write("{\"code\":\"ERR_IDEMPOTENT_REPLAY\",\"message\":\"Duplicate request detected for idempotency key\"}");
                return false;
            }
        } catch (Exception e) {
            // If Redis is temporarily unreachable, allow request to proceed
            return true;
        }

        return true;
    }
}
