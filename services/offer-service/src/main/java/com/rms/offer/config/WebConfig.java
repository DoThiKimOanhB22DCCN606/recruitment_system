package com.rms.offer.config;

import com.rms.offer.interceptor.IdempotencyInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final IdempotencyInterceptor idempotencyInterceptor;

    public WebConfig(IdempotencyInterceptor idempotencyInterceptor) {
        this.idempotencyInterceptor = idempotencyInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(idempotencyInterceptor)
                .addPathPatterns("/api/v1/offers"); // Only apply to creation endpoint
    }
}
