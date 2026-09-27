package com.recruitment.ats.config;

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
        String userName = request.getHeader("X-User-Name");
        if (userName == null || userName.isBlank()) {
            userName = request.getHeader("X-User-Email");
        }

        try {
            if (userIdStr != null && !userIdStr.isBlank()) {
                UUID userId = UUID.fromString(userIdStr);
                UUID tenantId = (tenantIdStr != null && !tenantIdStr.isBlank()) ? UUID.fromString(tenantIdStr) : null;
                String userRole = (role != null && !role.isBlank()) ? role : "CANDIDATE";
                String authRole = userRole.startsWith("ROLE_") ? userRole : "ROLE_" + userRole;
                List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority(authRole),
                        new SimpleGrantedAuthority(userRole)
                );

                UserPrincipal principal = new UserPrincipal(userId, tenantId, userRole);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
                RequestContext.set(userId, tenantId, userRole, userName);
            }
            filterChain.doFilter(request, response);
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid identity headers");
        } finally {
            RequestContext.clear();
        }
    }
}
