package com.recruitment.candidate.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class InternalSecretFilterTest {

    private static final String VALID_SECRET = "candidate-secret-xyz";
    private InternalSecretFilter secretFilter;

    @BeforeEach
    void setUp() {
        secretFilter = new InternalSecretFilter(VALID_SECRET);
    }

    @Test
    void rejectsInternalCvRequestWithoutSecret() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/cvs/1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        secretFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("ERR_UNAUTHORIZED");
    }

    @Test
    void allowsInternalCvRequestWithValidSecret() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/cvs/1");
        request.addHeader("X-Internal-Secret", VALID_SECRET);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        secretFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
