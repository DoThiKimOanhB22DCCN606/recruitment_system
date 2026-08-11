package com.recruitment.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            @Value("${app.security.dev-mode:false}") boolean devMode) {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable);

        if (devMode) {
            http.authorizeExchange(exchange -> exchange.anyExchange().permitAll());
        } else {
            http.authorizeExchange(exchange -> exchange
                    .pathMatchers("/actuator/health").permitAll()
                    .anyExchange().authenticated());
            http.oauth2ResourceServer(resourceServer -> resourceServer.jwt(jwt -> {
            }));
        }
        return http.build();
    }
}
