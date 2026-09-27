package com.recruitment.job.interceptor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyInterceptorTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private IdempotencyInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new IdempotencyInterceptor(redisTemplate);
    }

    @Test
    void nonPostRequestsAlwaysAllowed() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/jobs");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }

    @Test
    void firstPostRequestSucceeds() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(any(), eq("processed"), any(Duration.class))).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/jobs");
        request.addHeader("Idempotency-Key", "req-123");
        request.addHeader("X-Tenant-Id", "tenant-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }

    @Test
    void duplicatePostRequestReturns409Conflict() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(any(), eq("processed"), any(Duration.class))).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/jobs");
        request.addHeader("Idempotency-Key", "req-123");
        request.addHeader("X-Tenant-Id", "tenant-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(409);
        assertThat(response.getContentAsString()).contains("ERR_IDEMPOTENT_REPLAY");
    }
}
