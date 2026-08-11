package com.recruitment.interview.config;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Value("${app.security.dev-mode:false}") boolean devMode) throws Exception {
        http.csrf(csrf -> csrf.disable());
        if (devMode) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        } else {
            http.authorizeHttpRequests(auth -> auth
                    .requestMatchers("/actuator/health").permitAll()
                    .anyRequest().authenticated());
            http.oauth2ResourceServer(resourceServer -> resourceServer.jwt(jwt -> {
            }));
        }
        return http.build();
    }

    @Component
    @Order(Ordered.HIGHEST_PRECEDENCE + 10)
    static class RequestContextFilter extends OncePerRequestFilter {
        private final boolean devMode;

        RequestContextFilter(@Value("${app.security.dev-mode:false}") boolean devMode) {
            this.devMode = devMode;
        }

        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain) throws ServletException, IOException {
            String userId = request.getHeader("X-User-Id");
            String tenantIdHeader = request.getHeader("X-Tenant-Id");
            if (devMode && userId == null) {
                userId = "00000000-0000-4000-8000-000000000001";
            }
            try {
                UUID tenantId = tenantIdHeader == null || tenantIdHeader.isBlank()
                        ? null : UUID.fromString(tenantIdHeader);
                RequestContext.set(UUID.fromString(userId), tenantId,
                        request.getHeader("X-User-Role"));
                filterChain.doFilter(request, response);
            } catch (IllegalArgumentException | NullPointerException exception) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid identity headers");
            } finally {
                RequestContext.clear();
            }
        }

        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
            return "/actuator/health".equals(request.getRequestURI());
        }
    }
}
