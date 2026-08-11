package com.rms.gateway.filter;

import com.rms.gateway.service.BlacklistCheckService;
import com.rms.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.UUID;

@Component
public class JwtValidationGatewayFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;
    private final BlacklistCheckService blacklistCheckService;

    // Public routes that do not require JWT validation
    private record RouteKey(HttpMethod method, String pathPattern) {}

    private final Set<RouteKey> publicPaths = Set.of(
            new RouteKey(HttpMethod.POST, "/api/v1/auth/register"),
            new RouteKey(HttpMethod.POST, "/api/v1/auth/login"),
            new RouteKey(HttpMethod.POST, "/api/v1/auth/refresh"),
            new RouteKey(HttpMethod.POST, "/api/v1/auth/verify-email"),
            new RouteKey(HttpMethod.POST, "/api/v1/auth/forgot-password"),
            new RouteKey(HttpMethod.POST, "/api/v1/auth/reset-password"),
            new RouteKey(HttpMethod.POST, "/api/v1/auth/resend-verification"),
            new RouteKey(HttpMethod.GET, "/api/v1/invitations/accept"),
            new RouteKey(HttpMethod.POST, "/api/v1/invitations/accept"),
            new RouteKey(HttpMethod.GET, "/api/v1/jobs"),
            new RouteKey(HttpMethod.GET, "/actuator/health")
    );

    public JwtValidationGatewayFilter(JwtUtil jwtUtil, BlacklistCheckService blacklistCheckService) {
        this.jwtUtil = jwtUtil;
        this.blacklistCheckService = blacklistCheckService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // STEP 0: Unconditionally strip spoofed headers
        ServerHttpRequest.Builder requestBuilder = request.mutate()
                .headers(headers -> {
                    headers.remove("X-User-Id");
                    headers.remove("X-Tenant-Id");
                    headers.remove("X-User-Role");
                    headers.remove("X-Client-Ip");
                });

        ServerWebExchange sanitizedExchange = exchange.mutate().request(requestBuilder.build()).build();

        if (isPublicPath(request)) {
            return chain.filter(sanitizedExchange);
        }

        if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            return onError(sanitizedExchange, "Missing Authorization header", HttpStatus.UNAUTHORIZED);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onError(sanitizedExchange, "Invalid Authorization header", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtUtil.getClaims(token);
            String userIdStr = claims.getSubject();
            String tenantIdStr = claims.get("tenantId", String.class);
            String role = claims.get("role", String.class);

            UUID userId = UUID.fromString(userIdStr);
            UUID tenantId = (tenantIdStr != null && !tenantIdStr.trim().isEmpty()) ? UUID.fromString(tenantIdStr) : null;

            return blacklistCheckService.isBlacklisted(userId, tenantId)
                    .flatMap(isBlacklisted -> {
                        if (isBlacklisted) {
                            return onError(sanitizedExchange, "Token blacklisted", HttpStatus.FORBIDDEN);
                        }

                        // Inject headers
                        requestBuilder.header("X-User-Id", userId.toString())
                                .header("X-User-Role", role);

                        if (tenantId != null) {
                            requestBuilder.header("X-Tenant-Id", tenantId.toString());
                        } else {
                            requestBuilder.header("X-Tenant-Id", ""); // SYS_ADMIN case
                        }

                        // Extract X-Client-Ip
                        String clientIp = extractClientIp(request);
                        if (clientIp != null) {
                            requestBuilder.header("X-Client-Ip", clientIp);
                        }

                        // Remove Authorization header before forwarding
                        requestBuilder.headers(h -> h.remove(HttpHeaders.AUTHORIZATION));

                        return chain.filter(sanitizedExchange.mutate().request(requestBuilder.build()).build());
                    });

        } catch (Exception e) {
            return onError(sanitizedExchange, "Token Expired or Invalid", HttpStatus.UNAUTHORIZED);
        }
    }

    private boolean isPublicPath(ServerHttpRequest request) {
        HttpMethod method = request.getMethod();
        String path = request.getURI().getPath();
        
        // Exact match
        if (publicPaths.contains(new RouteKey(method, path))) {
            return true;
        }
        
        // Prefix match for /api/v1/jobs/{id}
        if (HttpMethod.GET.equals(method) && path.startsWith("/api/v1/jobs/")) {
            return true;
        }
        
        return false;
    }

    private String extractClientIp(ServerHttpRequest request) {
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.trim().isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }
        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }
        return null;
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        exchange.getResponse().setStatusCode(httpStatus);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1; // Run before routing
    }
}
