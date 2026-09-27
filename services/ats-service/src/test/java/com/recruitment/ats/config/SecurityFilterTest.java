package com.recruitment.ats.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SecurityFilterTest {

    private static final String VALID_SECRET = "test-internal-secret-123";

    private InternalHeaderAuthFilter authFilter;
    private InternalSecretFilter secretFilter;

    @BeforeEach
    void setUp() {
        authFilter = new InternalHeaderAuthFilter();
        secretFilter = new InternalSecretFilter(VALID_SECRET);
        SecurityContextHolder.clearContext();
        RequestContext.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContext.clear();
    }

    @Test
    void internalHeaderAuthFilterSetsSecurityContextAndRequestContext() throws ServletException, IOException {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/applications");
        request.addHeader("X-User-Id", userId.toString());
        request.addHeader("X-Tenant-Id", tenantId.toString());
        request.addHeader("X-User-Role", "HR_ADMIN");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = (req, res) -> {
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                    .anyMatch(a -> a.getAuthority().equals("ROLE_HR_ADMIN"));
            assertThat(RequestContext.userId()).isEqualTo(userId);
            assertThat(RequestContext.tenantId()).isEqualTo(tenantId);
            assertThat(RequestContext.role()).isEqualTo("HR_ADMIN");
        };

        authFilter.doFilter(request, response, filterChain);
    }

    @Test
    void internalHeaderAuthFilterDoesNotAuthenticateWhenUserIdMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/applications");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = (req, res) -> {
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            assertThat(RequestContext.currentUserId()).isNull();
        };

        authFilter.doFilter(request, response, filterChain);
    }

    @Test
    void internalSecretFilterRejectsInternalRequestWithoutSecret() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/applications/1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        secretFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("ERR_UNAUTHORIZED");
    }

    @Test
    void internalSecretFilterRejectsInternalRequestWithWrongSecret() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/internal/applications/1/hire");
        request.addHeader("X-Internal-Secret", "wrong-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        secretFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("ERR_UNAUTHORIZED");
    }

    @Test
    void internalSecretFilterAllowsInternalRequestWithValidSecret() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/applications/1");
        request.addHeader("X-Internal-Secret", VALID_SECRET);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        secretFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void internalSecretFilterPassesNonInternalRequestDirectly() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/applications");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        secretFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
