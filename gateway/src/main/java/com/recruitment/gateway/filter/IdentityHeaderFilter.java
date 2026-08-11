package com.recruitment.gateway.filter;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class IdentityHeaderFilter implements GlobalFilter, Ordered {

    private final boolean devMode;
    private final String devUserId;
    private final String devTenantId;
    private final String devRole;

    public IdentityHeaderFilter(
            @Value("${app.security.dev-mode:false}") boolean devMode,
            @Value("${app.security.dev-user-id:00000000-0000-4000-8000-000000000001}") String devUserId,
            @Value("${app.security.dev-tenant-id:00000000-0000-4000-8000-000000000100}") String devTenantId,
            @Value("${app.security.dev-role:RECRUITER}") String devRole) {
        this.devMode = devMode;
        this.devUserId = devUserId;
        this.devTenantId = devTenantId;
        this.devRole = devRole;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
                .cast(Authentication.class)
                .defaultIfEmpty(new AnonymousAuthentication())
                .flatMap(authentication -> {
                    Identity identity = resolveIdentity(authentication);
                    ServerHttpRequest request = exchange.getRequest().mutate()
                            .headers(headers -> {
                                headers.remove("X-User-Id");
                                headers.remove("X-Tenant-Id");
                                headers.remove("X-User-Role");
                                headers.remove("X-Client-IP");
                                headers.remove("X-Client-Device");
                                headers.add("X-User-Id", identity.userId());
                                if (identity.tenantId() != null && !identity.tenantId().isBlank()) {
                                    headers.add("X-Tenant-Id", identity.tenantId());
                                }
                                headers.add("X-User-Role", identity.role());
                                headers.add("X-Client-IP", clientIp(exchange));
                                headers.add("X-Client-Device", clientDevice(exchange));
                            })
                            .build();
                    return chain.filter(exchange.mutate().request(request).build());
                });
    }

    private Identity resolveIdentity(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwt) {
            String userId = jwt.getToken().getClaimAsString("user_id");
            String tenantId = jwt.getToken().getClaimAsString("tenant_id");
            List<String> roles = jwt.getToken().getClaimAsStringList("roles");
            String role = roles == null || roles.isEmpty() ? "USER" : roles.get(0);
            return new Identity(userId, tenantId, role);
        }
        if (devMode) {
            return new Identity(devUserId, blankToNull(devTenantId), devRole);
        }
        return new Identity("", "", "");
    }

    private String clientIp(ServerWebExchange exchange) {
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return exchange.getRequest().getRemoteAddress() == null
                ? "unknown"
                : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
    }

    private String clientDevice(ServerWebExchange exchange) {
        String userAgent = exchange.getRequest().getHeaders().getFirst(HttpHeaders.USER_AGENT);
        return userAgent == null || userAgent.isBlank() ? "unknown" : userAgent;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private record Identity(String userId, String tenantId, String role) {
    }

    private static final class AnonymousAuthentication implements Authentication {
        @Override public List<org.springframework.security.core.GrantedAuthority> getAuthorities() { return List.of(); }
        @Override public Object getCredentials() { return ""; }
        @Override public Object getDetails() { return null; }
        @Override public Object getPrincipal() { return ""; }
        @Override public boolean isAuthenticated() { return false; }
        @Override public void setAuthenticated(boolean authenticated) { }
        @Override public String getName() { return "anonymous"; }
    }
}
