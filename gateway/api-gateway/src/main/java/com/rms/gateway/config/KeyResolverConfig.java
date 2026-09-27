package com.rms.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.util.List;

@Configuration
public class KeyResolverConfig {

    @Bean
    @Primary
    public KeyResolver clientIpKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                return Mono.just("user:" + userId);
            }
            if (exchange.getRequest().getRemoteAddress() != null) {
                return Mono.just("ip:" + exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
            }
            List<String> forwardedFor = exchange.getRequest().getHeaders().get("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isEmpty()) {
                String ip = forwardedFor.get(0).split(",")[0].trim();
                return Mono.just("ip:" + ip);
            }
            return Mono.just("ip:unknown");
        };
    }
}
