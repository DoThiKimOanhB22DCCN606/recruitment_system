package com.rms.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.List;

@Configuration
public class KeyResolverConfig {

    @Bean
    public KeyResolver clientIpKeyResolver() {
        return exchange -> {
            List<String> forwardedFor = exchange.getRequest().getHeaders().get("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isEmpty()) {
                // Extract leftmost IP (client IP) to mitigate trivial spoofing if exposed
                String ip = forwardedFor.get(0).split(",")[0].trim();
                return Mono.just(ip);
            }
            return Mono.just(
                    exchange.getRequest().getRemoteAddress() != null
                            ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                            : "unknown"
            );
        };
    }
}
