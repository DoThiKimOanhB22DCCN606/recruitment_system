package com.recruitment.job.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final InternalHeaderAuthFilter internalHeaderAuthFilter;
    private final InternalSecretFilter internalSecretFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/internal/**", "/api/v1/internal/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/jobs", "/jobs/search", "/jobs/{id}", "/api/v1/jobs", "/api/v1/jobs/search", "/api/v1/jobs/{id}").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(internalHeaderAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(internalSecretFilter, InternalHeaderAuthFilter.class);

        return http.build();
    }
}
