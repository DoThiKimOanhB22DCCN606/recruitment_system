package com.recruitment.job.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class InternalSecretFilter extends OncePerRequestFilter {

    private final String internalSecret;

    public InternalSecretFilter(
            @Value("${app.internal.secret:${rms.internal-secret:${INTERNAL_SECRET:uuid-internal-secret-12345}}}") String internalSecret) {
        this.internalSecret = internalSecret;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (uri.startsWith("/internal") || uri.startsWith("/api/v1/internal")) {
            String secretHeader = request.getHeader("X-Internal-Secret");
            if (secretHeader == null || !secretHeader.equals(internalSecret)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"ERR_UNAUTHORIZED\",\"message\":\"Invalid or missing internal secret\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
