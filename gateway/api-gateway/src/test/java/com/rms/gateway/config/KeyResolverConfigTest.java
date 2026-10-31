package com.rms.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;

class KeyResolverConfigTest {

    private final KeyResolver keyResolver = new KeyResolverConfig().clientIpKeyResolver();

    @Test
    void resolvesUserIdWhenHeaderPresent() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/jobs")
                .header("X-User-Id", "user-uuid-123")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(keyResolver.resolve(exchange))
                .expectNext("user:user-uuid-123")
                .verifyComplete();
    }

    @Test
    void resolvesClientIpWhenUserIdMissing() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/jobs")
                .remoteAddress(new InetSocketAddress("192.168.1.100", 8080))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(keyResolver.resolve(exchange))
                .expectNext("ip:192.168.1.100")
                .verifyComplete();
    }
}