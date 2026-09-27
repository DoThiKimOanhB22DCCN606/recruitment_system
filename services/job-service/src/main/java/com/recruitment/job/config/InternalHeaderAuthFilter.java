package com.recruitment.job.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class InternalHeaderAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String userIdStr = request.getHeader("X-User-Id");
        String tenantIdStr = request.getHeader("X-Tenant-Id");
        String role = request.getHeader("X-User-Role");

        if (userIdStr != null && !userIdStr.isBlank() && role != null && !role.isBlank()) {
            try {
                UUID userId = UUID.fromString(userIdStr);
                UUID tenantId = (tenantIdStr != null && !tenantIdStr.isBlank()) ? UUID.fromString(tenantIdStr) : null;
                
                String authRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority(authRole),
                        new SimpleGrantedAuthority(role)
                );

                UserPrincipal principal = new UserPrincipal(userId, tenantId, role);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (IllegalArgumentException e) {
                // Invalid UUID format
            }
        }

        filterChain.doFilter(request, response);
    }
}
