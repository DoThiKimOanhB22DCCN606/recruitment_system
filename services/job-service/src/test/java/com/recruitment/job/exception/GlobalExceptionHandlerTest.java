package com.recruitment.job.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleIllegalArgumentReturnsBadRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/jobs");

        IllegalArgumentException ex = new IllegalArgumentException("Minimum salary cannot exceed maximum salary");
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo("Minimum salary cannot exceed maximum salary");
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/jobs");
    }
}
