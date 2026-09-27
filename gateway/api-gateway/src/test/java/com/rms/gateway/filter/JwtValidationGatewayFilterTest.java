package com.rms.gateway.filter;

import com.rms.gateway.service.BlacklistCheckService;
import com.rms.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtValidationGatewayFilterTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private BlacklistCheckService blacklistCheckService;
    @Mock private GatewayFilterChain chain;

    private JwtValidationGatewayFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtValidationGatewayFilter(jwtUtil, blacklistCheckService);
    }

    @Test
    void publicAuthRouteBypassesJwtValidation() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/auth/login").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(any(ServerWebExchange.class));
        verifyNoInteractions(jwtUtil, blacklistCheckService);
    }

    @Test
    void publicCompanyRouteBypassesJwtValidation() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/companies/123").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(any(ServerWebExchange.class));
        verifyNoInteractions(jwtUtil, blacklistCheckService);
    }

    @Test
    void privateRouteWithoutAuthHeaderReturns401() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/applications").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(chain);
    }

    @Test
    void validJwtInjectsUserHeadersAndStripsAuthorization() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String token = "valid-token";

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/applications")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Claims claims = new DefaultClaims(Map.of(
                "sub", userId.toString(),
                "tenantId", tenantId.toString(),
                "role", "HR_ADMIN"
        ));

        when(jwtUtil.getClaims(token)).thenReturn(claims);
        when(blacklistCheckService.isBlacklisted(userId, tenantId)).thenReturn(Mono.just(false));
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());

        HttpHeaders forwardedHeaders = captor.getValue().getRequest().getHeaders();
        assertThat(forwardedHeaders.getFirst("X-User-Id")).isEqualTo(userId.toString());
        assertThat(forwardedHeaders.getFirst("X-Tenant-Id")).isEqualTo(tenantId.toString());
        assertThat(forwardedHeaders.getFirst("X-User-Role")).isEqualTo("HR_ADMIN");
        assertThat(forwardedHeaders.getFirst(HttpHeaders.AUTHORIZATION)).isNull();
    }
}